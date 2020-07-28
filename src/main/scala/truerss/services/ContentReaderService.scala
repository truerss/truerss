package truerss.services

import truerss.db.Predefined
import truerss.dto.{FeedContent, FeedDto, SetupKey}
import zio.Task

class ContentReaderService(
                            feedsService: FeedsService,
                            readerClient: ReaderClient
                          ) {

  def fetchFeedContent(feedId: Long): Task[FeedContent] = {
    for {
      feed <- feedsService.findOne(feedId)
      content <- readFeedContent(feedId, feed)
    } yield FeedContent(content)
  }

  protected def readFeedContent(feedId: Long, feed: FeedDto): Task[Option[String]] = {
    feed.content match {
      case Some(_) => Task.succeed(feed.content)
      case None =>
        for {
          content <- readerClient.read(feed.url)
          _ <- updateContentIfNeed(feedId, content)
        } yield content
    }
  }

  protected def updateContentIfNeed(feedId: Long, contentOpt: Option[String]): Task[Unit] = {
    contentOpt match {
      case Some(content) =>
        feedsService.updateContent(feedId, content)
      case None =>
        Task.succeed(())
    }
  }

}

object ContentReaderService {

  val readContentKey: SetupKey = Predefined.read.toKey
  val defaultIsRead: Boolean = Predefined.read.value.defaultValue.asInstanceOf[Boolean]

  case class ReadResult(feedDto: FeedDto)

}