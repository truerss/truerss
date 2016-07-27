package truerss.system.actors

import akka.actor._
import truerss.controllers.{InternalServerErrorResponse, ModelResponse}
import truerss.models.Feed
import truerss.system.db
import truerss.system.network._
import truerss.system.util.FeedContentUpdate

class GetFeedActor(override val dbRef: ActorRef, sourcesRef: ActorRef)
  extends CommonActor {

  import db._

  var feed: Feed = null

  def receive = {
    case msg: GetFeed =>
      originalSender = sender
      dbRef ! msg

    case ResponseMaybeFeed(maybeFeed) =>
      maybeFeed match {
        case Some(f) =>
          feed = f
          feed.content match {
            case Some(content) =>
              originalSender ! ModelResponse(feed)
              context.stop(self)

            case None =>
              feed.id.foreach { feedId =>
                sourcesRef ! ExtractContent(feed.sourceId, feedId, feed.url)
              }
          }

        case None =>
          originalSender ! feedNotFound
          context.stop(self)
      }

    case response: NetworkResult =>
      val r = response match {
        case ExtractedEntries(sourceId, xs) =>
          InternalServerErrorResponse("Unexpected message")
        case ExtractContentForEntry(sourceId, feedId, content) =>
          content match {
            case Some(cnt) =>
              stream.publish(FeedContentUpdate(feedId, cnt))
              ModelResponse(feed.copy(content = Some(cnt)))
            case None => ModelResponse(feed)
          }
        case ExtractError(error) =>
          log.error(s"error on extract from ${feed.sourceId} -> ${feed.url}: $error")
          InternalServerErrorResponse(error)

        case SourceNotFound(sourceId) =>
          log.error(s"source $sourceId not found")
          InternalServerErrorResponse(s"source $sourceId not found")
      }
      originalSender ! r
      context.stop(self)

  }

}

object GetFeedActor {
  def props(dbRef: ActorRef, sourcesRef: ActorRef) =
    Props(classOf[GetFeedActor], dbRef, sourcesRef)
}