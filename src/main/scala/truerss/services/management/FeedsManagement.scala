package truerss.services.management

import akka.event.EventStream
import truerss.api._
import truerss.db.Predefined
import truerss.dto.{FeedContent, FeedDto, Page, SetupKey}
import truerss.services.actors.events.PublishPluginActor
import truerss.services.{ContentReaderService, FeedsService, SettingsService}
import truerss.util.syntax

import scala.concurrent.{ExecutionContext, Future}


class FeedsManagement(feedsService: FeedsService,
                      contentReaderService: ContentReaderService,
                      settingsService: SettingsService,
                      stream: EventStream
                     )
                     (implicit ec: ExecutionContext) extends BaseManagement {

  import FeedsManagement._
  import ResponseHelpers.ok
  import syntax.future._

  def markAll: R = {
    feedsService.markAllAsRead.map { _ => ok }
  }

  def favorites(offset: Int, limit: Int): R = {
    feedsService.favorites(offset, limit).map(toPage)
  }

  def getFeedContent(feedId: Long): R = {
    fetchFeed(feedId, forceReadContent = true).map {
      case FeedResponse(dto) =>
        FeedContentResponse(FeedContent(dto.content))
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

  def latest(offset: Int, limit: Int): R = {
    feedsService.latest(offset, limit).map(toPage)
  }

  private def feedHandler(feed: Option[FeedDto]) = {
    feed match {
      case Some(f) => FeedResponse(f)
      case None => ResponseHelpers.feedNotFound
    }
  }

  private def fetchFeed(feedId: Long, forceReadContent: Boolean): R = {
    feedsService.findOne(feedId).flatMap {
      case Some(feed) =>
        feed.content match {
          case Some(_) => FeedResponse(feed).toF

          case None =>
            // should read anyway
            if (forceReadContent) {
              processContent(feedId, feed)
            } else {
              settingsService.where[Boolean](readContentKey, defaultIsRead).flatMap { setup =>
                logger.debug(s"${readContentKey.name} is ${setup.value}")
                // skip then
                if (setup.value) {
                  FeedResponse(feed).toF
                } else {
                  processContent(feedId, feed)
                }
              }
            }
        }

      case None =>
        ResponseHelpers.feedNotFound.toF
    }
  }

  private def processContent(feedId: Long, feed: FeedDto): Future[Response] = {
    logger.debug(s"Need to read content for $feedId")
    fetchContentOrError(feed.url) { value =>
      val x = value.map { content =>
        updateContent(feedId, content)
        feed.copy(content = Some(content))
      }.getOrElse(feed)
      FeedResponse(x)
    }.toF
  }

  private def updateContent(feedId: Long, content: String): Unit = {
    feedsService.updateContent(feedId, content)
  }

  private def fetchContentOrError(url: String)(f: Option[String] => Response): Response = {
    contentReaderService.read(url).fold(InternalServerErrorResponse, f)
  }

}

object FeedsManagement {
  def toPage(tmp: (Vector[FeedDto], Int)): FeedsPageResponse = {
    FeedsPageResponse(Page[FeedDto](tmp._2, tmp._1))
  }

  val readContentKey: SetupKey = Predefined.read.toKey
  val defaultIsRead: Boolean = Predefined.read.value.defaultValue.asInstanceOf[Boolean]
}