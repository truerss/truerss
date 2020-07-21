package truerss.services

import java.net.URL

import com.github.truerss.base.aliases.WithContent
import com.github.truerss.base.{ContentTypeParam, Errors}
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import truerss.db.Predefined
import truerss.dto.{FeedContent, FeedDto, SetupKey}
import truerss.util.{Request, syntax}
import zio.Task

import scala.concurrent.ExecutionContext

class ContentReaderService(
                            feedsService: FeedsService,
                            applicationPluginsService: ApplicationPluginsService,
                            settingsService: SettingsService
                          )(implicit ec: ExecutionContext) {

  import ContentReaderService._
  import syntax.ext._

  protected val logger = LoggerFactory.getLogger(getClass)

  def fetchFeedContent(feedId: Long): Task[FeedContent] = {
    for {
      feed <- feedsService.findOne(feedId)
      result <- readFeedContent(feedId, feed, forceReadContent = true)
      _ <- Task.fail(ContentReadError(result.error.getOrElse(""))).when(result.hasError) // todo
    } yield FeedContent(result.feedDto.content)
  }

  def readFeedContent(feedId: Long, feed: FeedDto, forceReadContent: Boolean): Task[ReadResult] = {
    feed.content match {
      case Some(_) => Task.succeed(ReadResult(feed))
      case None =>
        if (forceReadContent) {
          processContent(feedId, feed)
        } else {
          for {
            setup <- settingsService.where[Boolean](readContentKey, defaultIsRead)
            // logger.debug(s"${readContentKey.name} is ${setup.value}")
            result <- if (setup.value) {
              Task.succeed(ReadResult(feed))
            } else {
              processContent(feedId, feed)
            }
          } yield result
        }
    }
  }

  private def processContent(feedId: Long, feed: FeedDto): Task[ReadResult] = {
    logger.debug(s"Need to read content for $feedId")
    read(feed.url).flatMap {
      case Left(error) =>
        logger.warn(s"Failed to fetch content: $error for the feed: $feedId")
        Task.succeed(ReadResult(error.some, feed))
      case Right(contentOpt) =>
        contentOpt match {
          case Some(content) =>
            for {
              _ <- feedsService.updateContent(feedId, content)
            } yield ReadResult(feed.copy(content = Some(content)))
          case None =>
            Task.succeed(ReadResult(feed))
        }

    }
  }

  protected def read(url: String): Task[Either[String, Option[String]]] = {
    val tmp = new URL(url)
    val c = applicationPluginsService.getContentReaderOrDefault(tmp)
      .asInstanceOf[WithContent]

    if (c.needUrl) {
      Task {
        pass(c.content(ContentTypeParam.UrlRequest(tmp)))
      }
    } else {
      for {
        value <- extractContent(url)
      } yield pass(c.content(ContentTypeParam.HtmlRequest(value)))
    }
  }

}

object ContentReaderService {

  val readContentKey: SetupKey = Predefined.read.toKey
  val defaultIsRead: Boolean = Predefined.read.value.defaultValue.asInstanceOf[Boolean]

  case class ReadResult(error: Option[String], feedDto: FeedDto) {
    def hasError: Boolean = {
      error.isDefined
    }
  }
  object ReadResult {
    def apply(feedDto: FeedDto): ReadResult = {
      new ReadResult(None, feedDto)
    }
  }

  private def extractContent(url: String): Task[String] = {
    // todo blocking thread ?
    for {
      response <- Task(Request.getResponse(url))
      _ <- Task.fail(ContentReadError(s"Connection error for $url")).when(response.isError)
      result <- Task(Jsoup.parse(response.body).body().html())
    } yield result
  }

  private def pass(result: Either[Errors.Error, Option[String]]): Either[String, Option[String]] = {
    result.fold(
      err => Left(err.error),
      x => Right(x)
    )
  }


}