package truerss.services.management

import akka.event.EventStream
import truerss.api.{BadRequestResponse, ImportResponse, Ok, Response}
import truerss.dto.{NewSourceDto, NewSourceFromFileWithErrors}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.services.{OpmlService, SourcesService}

import scala.concurrent.{ExecutionContext, Future}

class OpmlManagement(opmlService: OpmlService,
                     sourcesService: SourcesService,
                     stream: EventStream
                    )
                    (implicit ec: ExecutionContext) extends BaseManagement {

  import OpmlManagement._

  def getOpml: R = {
    opmlService.build.map(Ok)
  }

  def createFrom(opml: String): R = {
    Future {
      opmlService.parse(opml).fold(
        error => {
          logger.warn(s"Failed to parse given text as opml: $error")
          Future.successful(BadRequestResponse(error))
        },
        xs => {
          logger.info(s"Materialize ${xs.size} outlines from given file")
          val fs = xs.map { x =>
            from(x.link, x.title, interval)
          }
          sourcesService.addSources(fs).map { xs =>
            xs.foreach { x => stream.publish(SourcesKeeperActor.NewSource(x)) }
            ImportResponse(xs.toVector)
          }
        }
      )
    }.flatMap(identity)
  }

}

object OpmlManagement {
  val interval = 8

  def from(url: String, name: String, interval: Int): NewSourceDto = {
    NewSourceDto(
      url = url,
      name = name,
      interval = interval
    )
  }
}