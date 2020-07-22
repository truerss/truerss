package truerss.services

import java.net.URL

import com.github.truerss.base.{ContentTypeParam, Errors}
import com.github.truerss.base.aliases.WithContent
import org.jsoup.Jsoup
import truerss.util.{DefaultParameters, Request}
import zio.{Task, ZIO}
import zio.blocking._

class ReaderClient(private val applicationPluginsService: ApplicationPluginsService) {

  import ReaderClient._

  def read(url: String): Task[Option[String]] = {
    val tmp = new URL(url)
    val plugin = applicationPluginsService.getContentReaderOrDefault(tmp)
      .asInstanceOf[WithContent]

    if (plugin.needUrl) {
      fromEither(plugin.content(ContentTypeParam.UrlRequest(tmp)))
    } else {
      for {
        value <- extractContent(url)
        result <- fromEither(plugin.content(ContentTypeParam.HtmlRequest(value)))
      } yield result
    }
  }

}

object ReaderClient {

  // todo blocking
  def extractContent(url: String): Task[String] = {
    for {
      response <- Task(Request.getResponse(url))
      _ <- Task.fail(ContentReadError(s"Connection error for $url")).when(response.isError)
      result <- Task(Jsoup.parse(response.body).body().html())
    } yield result
  }

  def fromEither(result: Either[Errors.Error, Option[String]]): Task[Option[String]] = {
    result.fold(
      err => Task.fail(ContentReadError(err.error)),
      Task.succeed(_)
    )
  }
}
