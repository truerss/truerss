package truerss.db.validation

import java.util.concurrent.Executors

import akka.dispatch.ExecutionContexts
import truerss.dto.SourceDto
import truerss.util.{Request, syntax}

import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.Try

// plugin should be present for non-rss/atom urls
// +xml is part of ContentType
class SourceUrlValidator extends Request {

  import syntax._
  import ext._
  import SourceUrlValidator._

  private val logger = LoggerFactory.getLogger(getClass)

  override protected val connectionTimeout: Int = 1000
  override protected val readTimeout: Int = 1000

  // todo configuration plz
  private implicit val ec = ExecutionContexts.fromExecutor(Executors.newFixedThreadPool(10))

  def validateUrl(dto: SourceDto): Either[String, SourceDto] = {
    logger.debug(s"Validate: ${dto.url}")
    Try {
      makeRequest(dto.url).get(contentTypeHeaderName).map(isValid)
    }.toOption.flatten match {
      case Some(true) => dto.right
      case _ => buildError(dto.url).left
    }
  }

  // returns only valid
  def validateUrls(dtos: Seq[SourceDto]): Future[Seq[SourceDto]] = {
    val xs = dtos.map { x =>
      Future(validateUrl(x))
    }
    Future.sequence(xs)
      .map { res =>
        res.filter(_.isRight).flatMap(_.toOption)
      }
  }

  protected def makeRequest(url: String): Map[String, String] = {
    getRequestHeaders(url)
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