package truerss.services

import akka.event.EventStream
import org.slf4j.LoggerFactory
import truerss.dto.{NewSourceDto, SourceViewDto}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.util.{OpmlBuilder, OpmlParser, Outline, syntax}
import zio.{IO, Task, ZIO}

import scala.concurrent.{ExecutionContext, Future}

class OpmlService(sourcesService: SourcesService, eventStream: EventStream)
                 (implicit ec: ExecutionContext) {

  import OpmlService._
  import syntax.{\/, ext, future}
  import ext._
  import future._

  private val logger = LoggerFactory.getLogger(getClass)

  def build: Task[String] = {
    Task.fromFuture{implicit ec => sourcesService.getAllForOpml}.map(OpmlBuilder.build)
  }

  def create(text: String): Task[Iterable[SourceViewDto]] = {
    for {
      xs <- OpmlParser.parse(text)
      fs = fromOutlines(xs)
      result <- sourcesService.addSources(fs)
      _ <- publish(result)
    } yield result
  }

  private def publish(xs: Iterable[SourceViewDto]): Task[Unit] = {
    Task {
      xs.foreach { x =>
        eventStream.publish(SourcesKeeperActor.NewSource(x))
      }
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