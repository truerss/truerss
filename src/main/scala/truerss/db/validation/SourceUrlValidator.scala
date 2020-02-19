package truerss.db.validation

import truerss.dto.SourceDto
import truerss.util.{Request, syntax}

import scala.util.Try

// plugin should be present for non-rss/atom urls
// +xml is part of ContentType
class SourceUrlValidator {
  import syntax._
  import ext._
  import SourceUrlValidator._

  def validateUrl(dto: SourceDto): Either[String, SourceDto] = {
    Try {
      makeRequest(dto.url).get(contentTypeHeaderName).map(isValid)
    }.toOption.flatten match {
      case Some(true) => dto.right
      case _ => buildError(dto.url).left
    }
  }

  protected def makeRequest(url: String): Map[String, String] = {
    Request.getRequestHeaders(url)
  }

  private def buildError(url: String) = {
    s"$url is not a valid RSS/Atom feed"
  }

}

object SourceUrlValidator {
  val contentTypeHeaderName = "Content-Type"

  def isValid(value: String): Boolean = {
    value.contains("xml")
  }
}