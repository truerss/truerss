package truerss.system.actors

import akka.actor._
import truerss.api.{InternalServerErrorResponse, ModelResponse}
import truerss.models.Feed
import truerss.system.{db, network, util}

class GetFeedActor(override val dbRef: ActorRef, sourcesRef: ActorRef)
  extends CommonActor {

  import db._
  import network._
  import util.FeedContentUpdate

  var feed: Feed = null

  def defaultHandler = {
    case msg: GetFeed =>
      originalSender = sender
      dbRef ! msg

    case ResponseMaybeFeed(maybeFeed) =>
      maybeFeed match {
        case Some(f) =>
          feed = f
          feed.content match {
            case Some(_) =>
              originalSender ! ModelResponse(feed)
              finish

            case None =>
              feed.id.foreach { feedId =>
                sourcesRef ! ExtractContent(feed.sourceId, feedId, feed.url)
              }
          }

        case None =>
          originalSender ! feedNotFound
          finish
      }

    case response: NetworkResult =>
      val r = response match {
        case ExtractedEntries(_, _) =>
          InternalServerErrorResponse("Unexpected message")
        case ExtractContentForEntry(_, feedId, content) =>
          content match {
            case Some(cnt) =>
              stream.publish(FeedContentUpdate(feedId, cnt))
              ModelResponse(feed.copy(content = Some(cnt)))
            case None =>
              ModelResponse(feed)
          }
        case ExtractError(error) =>
          log.error(s"error on extract from ${feed.sourceId} -> ${feed.url}: $error")
          InternalServerErrorResponse(error)

        case SourceNotFound(sourceId) =>
          log.error(s"source $sourceId not found")
          InternalServerErrorResponse(s"source $sourceId not found")
      }
      originalSender ! r
      finish

  }

}

object GetFeedActor {
  def props(dbRef: ActorRef, sourcesRef: ActorRef) =
    Props(classOf[GetFeedActor], dbRef, sourcesRef)
}