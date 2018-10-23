package truerss.services

import java.net.URL

import com.github.truerss.base.{ContentTypeParam, Errors}
import com.github.truerss.base.aliases.WithContent
import org.jsoup.Jsoup
import truerss.util.Request

import scala.util.{Failure, Success, Try}

class ContentReaderService(applicationPluginsService: ApplicationPluginsService) {

  import ContentReaderService._

  def read(url: String): Either[String, Option[String]] = {
    val tmp = new URL(url)
    val c = applicationPluginsService.getContentReaderOrDefault(tmp)
      .asInstanceOf[WithContent]

    if (c.needUrl) {
      pass(c.content(ContentTypeParam.UrlRequest(tmp)))
    } else {
      Try {
        extractContent(url)
      } match {
        case Success(value) =>
          pass(c.content(ContentTypeParam.HtmlRequest(value)))

        case Failure(exception) =>
          Left(exception.getMessage)
      }
    }

  }

  private def pass(result: Either[Errors.Error, Option[String]]): Either[String, Option[String]] = {
    result.fold(
      err => Left(err.error),
      x => Right(x)
    )
  }

}

object ContentReaderService {
  def extractContent(url: String): String = {
    val response = Request.getResponse(url)

    if (response.isError) {
      throw new RuntimeException(s"Connection error for $url")
    }

    Jsoup.parse(response.body).body().html()
  }
}