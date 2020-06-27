package truerss.services

import akka.event.EventStream
import org.slf4j.LoggerFactory
import truerss.dto.{NewSourceDto, SourceViewDto}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.util.{OpmlBuilder, OpmlParser, Outline, syntax}

import scala.concurrent.{ExecutionContext, Future}

class OpmlService(sourcesService: SourcesService, eventStream: EventStream)(implicit ec: ExecutionContext) {

  import OpmlService._
  import syntax.{\/, ext, future}
  import ext._
  import future._

  private val logger = LoggerFactory.getLogger(getClass)

  def build: Future[String] = {
    sourcesService.getAllForOpml.map(OpmlBuilder.build)
  }

  def create(text: String): Future[String \/ Iterable[SourceViewDto]] = {
    OpmlParser.parse(text).fold(
      error => {
        logger.warn(s"Failed to parse given text as opml: $error")
        error.left.toF
      },
      xs => {
        logger.info(s"Materialize ${xs.size} outlines from given file")
        val fs = fromOutlines(xs)
        sourcesService.addSources(fs).map { result =>
          publish(result)
          result.right
        }
      }
    )
  }

  private def publish(xs: Iterable[SourceViewDto]): Unit = {
    xs.foreach { x =>
      eventStream.publish(SourcesKeeperActor.NewSource(x))
    }
  }

}

object OpmlService {
  val interval = 8

  def from(url: String, name: String, interval: Int): NewSourceDto = {
    NewSourceDto(
      url = url,
      name = name,
      interval = interval
    )
  }

  def fromOutlines(xs: Iterable[Outline]): Iterable[NewSourceDto] = {
    xs.map { x =>
      from(x.link, x.title, interval)
    }
  }
}