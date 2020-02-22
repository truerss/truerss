package truerss.services.management

import akka.event.EventStream
import truerss.api.{FeedResponse, FeedsPageResponse, FeedsResponse, InternalServerErrorResponse, Response}
import truerss.dto.FeedDto
import truerss.services.DbHelperActor.FeedContentUpdate
import truerss.services.{ContentReaderService, FeedsService, PublishPluginActor}
import truerss.util.Util.ResponseHelpers
import truerss.util.Util.ResponseHelpers.{feedNotFound, ok}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}


class FeedsManagement(feedsService: FeedsService,
                      contentReaderService: ContentReaderService,
                      stream: EventStream
                     )
                     (implicit ec: ExecutionContext){

  private val logger = LoggerFactory.getLogger(getClass)

  type R = Future[Response]

  def markAll: R = {
    feedsService.markAllAsRead.map { _ => ok }
  }

  def favorites: R = {
    feedsService.favorites.map(FeedsResponse)
  }

  def getFeed(feedId: Long): R = {
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
        logger.warn(s"Failed to get content for feed=$feedId: $ex")
        InternalServerErrorResponse("Something went wrong")
    }
  }

  def addToFavorites(feedId: Long): R = {
    feedsService.addToFavorites(feedId).map {
      case Some(feed) =>
        stream.publish(PublishPluginActor.PublishEvent(feed))
        FeedResponse(feed)
      case None =>
        feedNotFound
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

  def fetchBySource(sourceId: Long, from: Int, limit: Int): R = {
    feedsService.findBySource(sourceId, from, limit).map { tmp =>
      FeedsPageResponse(tmp._1, tmp._2)
    }
  }

  def latest(count: Int): R = {
    feedsService.latest(count).map(FeedsResponse)
  }


  private def feedHandler(feed: Option[FeedDto]) = {
    feed match {
      case Some(f) => FeedResponse(f)
      case None => ResponseHelpers.feedNotFound
    }
  }

}