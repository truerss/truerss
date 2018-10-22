package truerss.services.actors.management

import akka.actor._
import akka.event.LoggingReceive
import akka.pattern._
import truerss.api.{InternalServerErrorResponse, ModelResponse}
import truerss.db.DbLayer
import truerss.db.Feed
import truerss.services.DbHelperActor.FeedContentUpdate
import truerss.services.actors.sync.SourceActor
import truerss.util.Util.ResponseHelpers

class GetFeedActor(dbLayer: DbLayer, service: ActorRef)
  extends CommonActor {

  import FeedsManagementActor.GetFeed
  import GetFeedActor._
  import context.dispatcher
  import ResponseHelpers._

  private var originalSender = context.system.deadLetters

  var feed: Feed = null

  def receive = LoggingReceive {
    case GetFeed(feedId) =>
      originalSender = sender

      val result = dbLayer.feedDao.findOne(feedId).map {
        case Some(f) => FeedPresent(f)
        case None =>
          NoEntity
      }

      result pipeTo self

    case NoEntity =>
      originalSender ! feedNotFound

    case FeedPresent(f) if f.content.isDefined =>
      originalSender ! ModelResponse(f)

    case FeedPresent(f) =>
      feed = f
      service ! SourceActor.ExtractContent(f.sourceId, f.id.get, f.url)

    case response: SourceActor.NetworkResult =>
      val r = response match {
        case SourceActor.ExtractedEntries(_, _) =>
          InternalServerErrorResponse("Unexpected message")
        case SourceActor.ExtractContentForEntry(_, feedId, content) =>
          content match {
            case Some(cnt) =>
              stream.publish(FeedContentUpdate(feedId, cnt))
              ModelResponse(feed.copy(content = Some(cnt)))
            case None =>
              ModelResponse(feed)
          }
        case SourceActor.ExtractError(error) =>
          log.warning(s"error on extract from ${feed.sourceId} -> ${feed.url}: $error")
          InternalServerErrorResponse(error)

        case SourceActor.SourceNotFound(sourceId) =>
          log.error(s"source $sourceId not found")
          InternalServerErrorResponse(s"source $sourceId not found")
      }
      originalSender ! r

    case x =>
      log.warning(s"Unhandled message when fetch feed content: $x")
      originalSender ! InternalServerErrorResponse("Something went wrong")

  }

}

object GetFeedActor {
  def props(dbLayer: DbLayer, service: ActorRef) =
    Props(classOf[GetFeedActor], dbLayer, service)

  case class FeedPresent(feed: Feed)
  case object NoEntity

}