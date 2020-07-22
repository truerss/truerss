package truerss.db.validation

import truerss.dto.SourceDto
import truerss.util.{Request, syntax}
import zio.{Task, ZIO}

import scala.util.Try

// plugin should be present for non-rss/atom urls
// +xml is part of ContentType
class SourceUrlValidator extends Request {

  import SourceUrlValidator._
  import syntax._
  import ext._

  override protected val connectionTimeout: Int = 1000
  override protected val readTimeout: Int = 1000

  def validateUrl(dto: SourceDto): Either[String, SourceDto] = {
    Try {
      makeRequest(dto.url).get(contentTypeHeaderName).map(isValid)
    }.toOption.flatten match {
      case Some(true) => dto.right
      case _ => buildError(dto.url).left
    }
  }

  // returns only valid
  // TODO pass from the top where[Setup]
  def validateUrls(dtos: Seq[SourceDto]): Task[Seq[SourceDto]] = {
    Task.collectParN(10)(dtos) { x =>
      ZIO.fromOption(validateUrl(x).toOption)
    }
  }

  protected def makeRequest(url: String): Map[String, String] = {
    getRequestHeaders(url)
  }

}

object SourceUrlValidator {
  val contentTypeHeaderName = "Content-Type"

  def isValid(value: String): Boolean = {
    value.contains("xml")
  }

  def buildError(url: String): String = {
    s"$url is not a valid RSS/Atom feed"
  }
}