package truerss.services.management

import akka.event.EventStream
import truerss.api._
import truerss.db.Predefined
import truerss.dto.{FeedContent, FeedDto, Page}
import truerss.services.DbHelperActor.FeedContentUpdate
import truerss.services.{ContentReaderService, FeedsService, PublishPluginActor, SettingsService}
import truerss.util.syntax.future._
import truerss.util.syntax.ext._

import scala.concurrent.{ExecutionContext, Future}


class FeedsManagement(feedsService: FeedsService,
                      contentReaderService: ContentReaderService,
                      settingsService: SettingsService,
                      stream: EventStream
                     )
                     (implicit ec: ExecutionContext) extends BaseManagement {

  import FeedsManagement._
  import ResponseHelpers.ok

  private val read = Predefined.read
  private val readContent = read.toKey

  def markAll: R = {
    feedsService.markAllAsRead.map { _ => ok }
  }

  def favorites: R = {
    feedsService.favorites.map(FeedsResponse)
  }

  def getFeedContent(feedId: Long): R = {
    fetchFeedOrError(feedId) { feed =>
      feed.content match {
        case Some(content) =>
          FeedContentResponse(FeedContent(content.some)).toF

        case None =>
          fetchContentOrError(feed.url) { maybeContent =>
            maybeContent.foreach { content =>
              stream.publish(FeedContentUpdate(feedId, content))
            }
            FeedContentResponse(FeedContent(maybeContent))
          }.toF
      }
    }
  }

  def getFeed(feedId: Long): R = {
    fetchFeedOrError(feedId) { feed =>
      feed.content match {
        case Some(_) =>
          FeedResponse(feed).toF

        case None =>
          processContent(feedId, feed)
      }
    }
  }

  private def fetchFeedOrError(feedId: Long)(f: FeedDto => Future[Response]): R = {
    feedsService.findOne(feedId).flatMap {
      case Some(feed) =>
        f.apply(feed)

      case None =>
        ResponseHelpers.feedNotFound.toF
    }.recover {
      case ex: Throwable =>
        logger.warn(s"Failed to get content for feed=$feedId: $ex")
        InternalServerErrorResponse("Something went wrong")
    }
  }

  private def processContent(feedId: Long, feed: FeedDto): Future[Response] = {
    settingsService.where[Boolean](readContent, true).map { setup =>
      if (setup.value) {
        logger.debug(s"Need to read content for $feedId")
        fetchContentOrError(feed.url) { value =>
          val x = value.map { content =>
            stream.publish(FeedContentUpdate(feedId, content))
            feed.copy(content = Some(content))
          }.getOrElse(feed)
          FeedResponse(x)
        }
      } else {
        FeedResponse(feed)
      }
    }
  }

  private def fetchContentOrError(url: String)(f: Option[String] => Response): Response = {
    contentReaderService.read(url).fold(
      error =>
        InternalServerErrorResponse(error),
      value => {
        f(value)
      }
    )
  }

  def addToFavorites(feedId: Long): R = {
    feedsService.addToFavorites(feedId).map {
      case Some(feed) =>
        stream.publish(PublishPluginActor.PublishEvent(feed))
        FeedResponse(feed)
      case None =>
        ResponseHelpers.feedNotFound
    }
  }

  def removeFromFavorites(feedId: Long): R = {
    feedsService.removeFromFavorites(feedId).map(feedHandler)
  }

  def markAsRead(feedId: Long): R = {
    feedsService.markAsRead(feedId).map(feedHandler)
  }

  def markAsUnread(feedId: Long): R = {
    feedsService.markAsUnread(feedId).map(feedHandler)
  }

  def findUnreadBySource(sourceId: Long): R = {
    feedsService.findUnread(sourceId).map(FeedsResponse)
  }

  def fetchBySource(sourceId: Long, unreadOnly: Boolean, offset: Int, limit: Int): R = {
    feedsService.findBySource(sourceId, unreadOnly, offset, limit).map(convert)
  }

  def latest(offset: Int, limit: Int): R = {
    feedsService.latest(offset, limit).map(convert)
  }

  private def feedHandler(feed: Option[FeedDto]) = {
    feed match {
      case Some(f) => FeedResponse(f)
      case None => ResponseHelpers.feedNotFound
    }
  }

}

object FeedsManagement {
  def convert(tmp: (Vector[FeedDto], Int)): FeedsPageResponse = {
    FeedsPageResponse(Page[FeedDto](tmp._2, tmp._1))
  }
}