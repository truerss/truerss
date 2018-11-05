package truerss.services.actors.management

import akka.actor.Props
import akka.pattern.pipe
import truerss.api._
import truerss.dto.FeedDto
import truerss.services.DbHelperActor.FeedContentUpdate
import truerss.services.{ContentReaderService, FeedsService, PublishPluginActor}
import truerss.util.Util.ResponseHelpers

/**
  * Created by mike on 4.5.17.
  */
class FeedsManagementActor(feedsService: FeedsService, contentReaderService: ContentReaderService) extends CommonActor {

  import FeedsManagementActor._
  import ResponseHelpers._
  import context.dispatcher

  override def receive: Receive = {
    case MarkAll =>
      feedsService.markAllAsRead.map { _ => ok } pipeTo sender

    case MarkFeed(feedId) =>
      feedsService.addToFavorites(feedId).map {
        case Some(feed) =>
          stream.publish(PublishPluginActor.PublishEvent(feed))
          FeedResponse(feed)
        case None =>
          feedNotFound
      } pipeTo sender

    case UnmarkFeed(feedId) =>
      feedsService.removeFromFavorites(feedId).map(feedHandler) pipeTo sender

    case MarkAsReadFeed(feedId) =>
      feedsService.markAsRead(feedId).map(feedHandler) pipeTo sender

    case MarkAsUnreadFeed(feedId) =>
      feedsService.markAsUnread(feedId).map(feedHandler) pipeTo sender

    case Unread(sourceId) =>
      feedsService.findUnread(sourceId).map(FeedsResponse) pipeTo sender

    case ExtractFeedsForSource(sourceId, from, limit) =>
      feedsService.findBySource(sourceId, from, limit).map { tmp =>
        FeedsPageResponse(tmp._1, tmp._2)
      } pipeTo sender

    case Latest(count) =>
      feedsService.latest(count).map(FeedsResponse) pipeTo sender

    case Favorites =>
      feedsService.favorites.map(FeedsResponse) pipeTo sender

    case GetFeed(feedId) =>
      feedsService.findOne(feedId).map {
        case Some(feed) =>
          feed.content match {
            case Some(_) =>
              FeedResponse(feed)

            case None =>
              contentReaderService.read(feed.url).fold(
                error =>
                  InternalServerErrorResponse(error),
                value => {
                  val x = value.map { content =>
                    stream.publish(FeedContentUpdate(feedId, content))
                    feed.copy(content = Some(content))
                  }.getOrElse(feed)
                  FeedResponse(x)
                }
              )
          }

        case None =>
          feedNotFound
      }.recover {
        case ex: Throwable =>
          log.warning(s"Failed to get content for feed=$feedId: $ex")
          InternalServerErrorResponse("Something went wrong")
      } pipeTo sender

  }

  private def feedHandler(feed: Option[FeedDto]) = {
    feed match {
      case Some(f) => FeedResponse(f)
      case None => ResponseHelpers.feedNotFound
    }
  }


}

object FeedsManagementActor {

  def props(feedsService: FeedsService, contentReaderService: ContentReaderService) = {
    Props(classOf[FeedsManagementActor], feedsService, contentReaderService)
  }

  sealed trait FeedsMessage
  case object MarkAll extends FeedsMessage

  case object Favorites extends FeedsMessage
  case class GetFeed(num: Long) extends FeedsMessage
  case class MarkFeed(num: Long) extends FeedsMessage
  case class UnmarkFeed(num: Long) extends FeedsMessage
  case class MarkAsReadFeed(num: Long) extends FeedsMessage
  case class MarkAsUnreadFeed(num: Long) extends FeedsMessage
  case class Latest(count: Int) extends FeedsMessage
  case class ExtractFeedsForSource(sourceId: Long,
                                   from: Int = 0,
                                   limit: Int = 100) extends FeedsMessage
  case class Unread(sourceId: Long) extends FeedsMessage

}