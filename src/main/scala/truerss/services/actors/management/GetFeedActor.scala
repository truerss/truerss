package truerss.services.actors.management

import akka.actor._
import akka.event.LoggingReceive
import akka.pattern._
import truerss.api.{FeedResponse, InternalServerErrorResponse, ModelResponse}
import truerss.db.DbLayer
import truerss.db.Feed
import truerss.dto.FeedDto
import truerss.services.DbHelperActor.FeedContentUpdate
import truerss.services.FeedsService
import truerss.services.actors.sync.SourceActor
import truerss.util.Util.ResponseHelpers

class GetFeedActor(feedsService: FeedsService, service: ActorRef)
  extends CommonActor {

  import FeedsManagementActor.GetFeed
  import GetFeedActor._
  import context.dispatcher
  import ResponseHelpers._

  private var originalSender = context.system.deadLetters

  var feed: FeedDto = null

  def receive = LoggingReceive {
    case GetFeed(feedId) =>
      originalSender = sender

      val result = feedsService.findOne(feedId).map {
        case Some(f) => FeedPresent(f)
        case None =>
          NoEntity
      }

      result pipeTo self

    case NoEntity =>
      originalSender ! feedNotFound

    case FeedPresent(f) if f.content.isDefined =>
      originalSender ! FeedResponse(f)

    case FeedPresent(f) =>
      feed = f
      service ! SourceActor.ExtractContent(f.sourceId, f.id, f.url)

    case response: SourceActor.NetworkResult =>
      val r = response match {
        case SourceActor.ExtractedEntries(_, _) =>
          InternalServerErrorResponse("Unexpected message")
        case SourceActor.ExtractContentForEntry(_, feedId, content) =>
          content match {
            case Some(cnt) =>
              stream.publish(FeedContentUpdate(feedId, cnt))
              FeedResponse(feed.copy(content = Some(cnt)))
            case None =>
              FeedResponse(feed)
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
  def props(feedsService: FeedsService, service: ActorRef) =
    Props(classOf[GetFeedActor], feedsService, service)

  case class FeedPresent(feed: FeedDto)
  case object NoEntity

}