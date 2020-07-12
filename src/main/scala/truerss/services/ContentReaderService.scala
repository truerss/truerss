package truerss.services

import java.net.URL

import com.github.truerss.base.{ContentTypeParam, Errors}
import com.github.truerss.base.aliases.WithContent
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import truerss.db.Predefined
import truerss.dto.{FeedDto, SetupKey}
import truerss.util.{Request, syntax}
import zio.Task

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ContentReaderService(
                            feedsService: FeedsService,
                            applicationPluginsService: ApplicationPluginsService,
                            settingsService: SettingsService
                          )(implicit ec: ExecutionContext) {

  import ContentReaderService._
  import syntax.future._
  import syntax.ext._

  protected val logger = LoggerFactory.getLogger(getClass)

  def readFeedContent(feedId: Long, feed: FeedDto, forceReadContent: Boolean): Task[ReadResult] = {
    feed.content match {
      case Some(_) => Task.succeed(ReadResult(feed))
      case None =>
        if (forceReadContent) {
          Task.fromFuture { implicit ec => processContent(feedId, feed) }
        } else {
          for {
            setup <- Task.fromFuture { implicit ec =>
              settingsService.where[Boolean](readContentKey, defaultIsRead)
            }
            // logger.debug(s"${readContentKey.name} is ${setup.value}")
            result <- if (setup.value) {
              Task.succeed(ReadResult(feed))
            } else {
              Task.fromFuture { implicit ec =>
                processContent(feedId, feed)
              }
            }
          } yield result
        }
    }
  }

  private def processContent(feedId: Long, feed: FeedDto): Future[ReadResult] = {
    logger.debug(s"Need to read content for $feedId")
    read(feed.url).fold(
      error => {
        logger.warn(s"Failed to fetch content: $error for the feed: $feedId")
        ReadResult(error.some, feed).toF
      },
      contentOpt => {
        contentOpt.map { content =>
          feedsService.updateContent(feedId, content)
          ReadResult(feed.copy(content = Some(content))).toF
        }.getOrElse(ReadResult(feed).toF)
      }
    )
  }

  protected def read(url: String): Either[String, Option[String]] = {
    val tmp = new URL(url)
    val c = applicationPluginsService.getContentReaderOrDefault(tmp)
      .asInstanceOf[WithContent]

    if (c.needUrl) {
      pass(c.content(ContentTypeParam.UrlRequest(tmp)))
    } else {
      extractContent(url) match {
        case Success(value) =>
          pass(c.content(ContentTypeParam.HtmlRequest(value)))

        case Failure(exception) =>
          Left(exception.getMessage)
      }
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

  private def extractContent(url: String): Try[String] = {
    val response = Request.getResponse(url)

    if (response.isError) {
      Failure(new RuntimeException(s"Connection error for $url"))
    } else {
      Success(Jsoup.parse(response.body).body().html())
    }
  }

  private def pass(result: Either[Errors.Error, Option[String]]): Either[String, Option[String]] = {
    result.fold(
      err => Left(err.error),
      x => Right(x)
    )
  }


}