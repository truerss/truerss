package truerss.services.management

import akka.event.EventStream
import truerss.api._
import truerss.dto.{FeedContent, FeedDto, Page}
import truerss.services.actors.events.PublishPluginActor
import truerss.services.{ContentReaderService, FeedsService}
import truerss.util.syntax

import scala.concurrent.ExecutionContext


class FeedsManagement(feedsService: FeedsService,
                      contentReaderService: ContentReaderService,
                      stream: EventStream
                     )
                     (implicit ec: ExecutionContext) extends BaseManagement {

  import FeedsManagement._
  import ResponseHelpers.ok
  import syntax.future._

  def markAll: R = {
    feedsService.markAllAsRead.map { _ => ok }
  }

  def favorites(offset: Int, limit: Int): Z = {
    feedsService.favorites(offset, limit).map(toPage)
  }

  def getFeedContent(feedId: Long): R = {
    fetchFeed(feedId, forceReadContent = true).map {
      case response: FeedResponse => toContent(response)
      case x => x
    }
  }

  def getFeed(feedId: Long): R = {
    fetchFeed(feedId, forceReadContent = false)
  }

  def changeFavorites(feedId: Long, favFlag: Boolean): R = {
    feedsService.changeFav(feedId, favFlag).map {
      case Some(feed) =>
        if (favFlag) {
          stream.publish(PublishPluginActor.PublishEvent(feed))
        }
        FeedResponse(feed)
      case None =>
        ResponseHelpers.feedNotFound
    }
  }

  def changeRead(feedId: Long, readFlag: Boolean): R = {
    feedsService.changeRead(feedId, readFlag).map(feedHandler)
  }

  def findUnreadBySource(sourceId: Long): R = {
    feedsService.findUnread(sourceId).map(FeedsResponse)
  }

  def fetchBySource(sourceId: Long, unreadOnly: Boolean, offset: Int, limit: Int): R = {
    feedsService.findBySource(sourceId, unreadOnly, offset, limit).map(toPage)
  }

  def latest(offset: Int, limit: Int): Z = {
    feedsService.latest(offset, limit).map(toPage)
  }

  private def fetchFeed(feedId: Long, forceReadContent: Boolean): R = {
    feedsService.findOne(feedId).flatMap {
      case Some(feed) =>
        contentReaderService.readFeedContent(feedId, feed, forceReadContent).map { result =>
          if (forceReadContent && result.hasError) {
            InternalServerErrorResponse(result.error.getOrElse(""))
          } else {
            FeedResponse(result.feedDto)
          }
        }

      case None =>
        ResponseHelpers.feedNotFound.toF
    }
  }

}

object FeedsManagement {
  def toPage(tmp: (Vector[FeedDto], Int)): FeedsPageResponse = {
    FeedsPageResponse(Page[FeedDto](tmp._2, tmp._1))
  }

  def toContent(response: FeedResponse): FeedContentResponse = {
    FeedContentResponse(FeedContent(response.dto.content))
  }

  private def feedHandler(feed: Option[FeedDto]) = {
    feed match {
      case Some(f) => FeedResponse(f)
      case None => ResponseHelpers.feedNotFound
    }
  }

}