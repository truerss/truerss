package truerss.db.validation

import org.slf4j.LoggerFactory
import truerss.dto.SourceDto
import truerss.http_support.Request
import truerss.util.syntax
import zio.{Task, ZIO}

import scala.util.Try

// plugin should be present for non-rss/atom urls
// +xml is part of ContentType
class SourceUrlValidator extends Request {

  import SourceUrlValidator._
  import syntax._
  import ext._

  protected val logger = LoggerFactory.getLogger(getClass)

  override protected val connectionTimeout: Int = 1000
  override protected val readTimeout: Int = 1000

  def validateUrl(dto: SourceDto): Either[String, SourceDto] = {
    Try {
      makeRequest(dto.url).get(contentTypeHeaderName).map(isValid)
    }.toOption.flatten match {
      case Some(true) => dto.right
      case x =>
        logger.warn(s"${dto.url} is not valid")
        buildError(dto.url).left
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