package truerss.services

import akka.event.EventStream
import truerss.dto.{NewSourceDto, SourceViewDto}
import truerss.services.OpmlService.fromOutlines
import truerss.services.OpmlServiceZ.OpmlServiceZ
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.util.{OpmlBuilder, OpmlParser, Outline}
import zio._

import scala.concurrent.ExecutionContext

object OpmlServiceZ {

  type OpmlServiceZ = Has[Service]

  trait Service {
    def build: Task[String]
    def create(text: String): Task[Iterable[SourceViewDto]]
  }

  final class Live(sourcesService: SourcesService, eventStream: EventStream)
                             (implicit ec: ExecutionContext) extends Service {
    def build: Task[String] = {
      sourcesService.getAllForOpml.map(OpmlBuilder.build)
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

  def build: ZIO[OpmlServiceZ, Throwable, String] = {
    ZIO.accessM(_.get.build)
  }

  def create(text: String): ZIO[OpmlServiceZ, Throwable, Iterable[SourceViewDto]] = {
    ZIO.accessM(_.get.create(text))
  }
}

class OpmlService(sourcesService: SourcesService, eventStream: EventStream)
                 (implicit ec: ExecutionContext) {

  type K[T] = ZIO[OpmlServiceZ.OpmlServiceZ, Throwable, T]

  private val layer: Layer[Nothing, OpmlServiceZ] =
    ZLayer.succeed(new OpmlServiceZ.Live(sourcesService, eventStream))

  def build: Task[String] = {
    val f: K[String] = OpmlServiceZ.build
    f.provideLayer(layer)
  }

  def create(text: String): Task[Iterable[SourceViewDto]] = {
    val f: K[Iterable[SourceViewDto]] = OpmlServiceZ.create(text)
    f.provideLayer(layer)
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