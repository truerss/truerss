package truerss.services.management

import akka.event.EventStream
import truerss.api._
import truerss.dto.{FeedContent, FeedDto, Page}
import truerss.services.{ContentReaderService, FeedsService}

class FeedsManagement(feedsService: FeedsService,
                      contentReaderService: ContentReaderService,
                      stream: EventStream
                     ) extends BaseManagement {

  import FeedsManagement._
  import ResponseHelpers.ok

  def getFeedContent(feedId: Long): Z = {
    fetchFeed(feedId, forceReadContent = true).map {
      case response: FeedResponse => toContent(response)
      case x => x
    }
  }

  def getFeed(feedId: Long): Z = {
    fetchFeed(feedId, forceReadContent = false)
  }

  def findUnreadBySource(sourceId: Long): Z = {
    feedsService.findUnread(sourceId).map(FeedsResponse)
  }

  private def fetchFeed(feedId: Long, forceReadContent: Boolean): Z = {
    for {
      feed <- feedsService.findOne(feedId)
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

  private def feedHandler(feed: FeedDto) = {
    FeedResponse(feed)
  }

}