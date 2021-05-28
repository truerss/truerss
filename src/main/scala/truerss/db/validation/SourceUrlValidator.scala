package truerss.db.validation

import org.slf4j.LoggerFactory
import truerss.http_support.Request
import truerss.util.syntax

import scala.util.Try

// plugin should be present for non-rss/atom urls
// +xml is part of ContentType
class SourceUrlValidator extends Request {

  import SourceUrlValidator._
  import SourceValidator.TmpSource
  import syntax._
  import ext._

  protected val logger = LoggerFactory.getLogger(getClass)

  override protected val connectionTimeout: Int = 1000
  override protected val readTimeout: Int = 1000

  def validateUrl(dto: TmpSource): Either[String, TmpSource] = {
    Try {
      makeRequest(dto.url).get(contentTypeHeaderName).map(isValid)
    }.toOption.flatten match {
      case Some(true) => dto.right
      case _ =>
        logger.warn(s"${dto.url} is not valid")
        buildError(dto.url).left
    }
  }

  protected def makeRequest(url: String): Map[String, String] = {
    getRequestHeaders(url).map(x => x._1.toLowerCase -> x._2)
  }

}

object SourceUrlValidator {
  val contentTypeHeaderName = "content-type"

  def isValid(value: String): Boolean = {
    value.contains("xml")
  }

  def buildError(url: String): String = {
    s"$url is not a valid RSS/Atom feed"
  }
}