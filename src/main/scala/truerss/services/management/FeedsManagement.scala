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

  def markAll: Z = {
    feedsService.markAllAsRead.map { _ => ok }
  }

  def favorites(offset: Int, limit: Int): Z = {
    feedsService.favorites(offset, limit).map(toPage)
  }

  def getFeedContent(feedId: Long): Z = {
    fetchFeed(feedId, forceReadContent = true).map {
      case response: FeedResponse => toContent(response)
      case x => x
    }
  }

  def getFeed(feedId: Long): Z = {
    fetchFeed(feedId, forceReadContent = false)
  }

  def changeFavorites(feedId: Long, favFlag: Boolean): Z = {
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

  def changeRead(feedId: Long, readFlag: Boolean): Z = {
    feedsService.changeRead(feedId, readFlag).map(feedHandler)
  }

  def findUnreadBySource(sourceId: Long): Z = {
    feedsService.findUnread(sourceId).map(FeedsResponse)
  }

  def fetchBySource(sourceId: Long, unreadOnly: Boolean, offset: Int, limit: Int): Z = {
    feedsService.findBySource(sourceId, unreadOnly, offset, limit).map(toPage)
  }

  def latest(offset: Int, limit: Int): Z = {
    feedsService.latest(offset, limit).map(toPage)
  }

  private def fetchFeed(feedId: Long, forceReadContent: Boolean): Z = {
    for {
      feed <- feedsService.findSingle(feedId)
      content <- contentReaderService
        .readFeedContent(feedId, feed, forceReadContent) // todo catch* unreadable content error
    } yield FeedResponse(content.feedDto)
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