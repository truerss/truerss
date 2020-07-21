package truerss.services.management

import akka.event.EventStream
import truerss.api._
import truerss.dto.{NewSourceDto, UpdateSourceDto}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.services.{OpmlService, SourceOverviewService, SourcesService}
import zio.Task

import scala.concurrent.ExecutionContext

class SourcesManagement(sourcesService: SourcesService,
                        opmlService: OpmlService,
                        sourceOverviewService: SourceOverviewService,
                        stream: EventStream
                       )
                       (implicit ec: ExecutionContext) extends BaseManagement {

  import ResponseHelpers.{ok, sourceNotFound}

  def addSource(dto: NewSourceDto): Z = {
    for {
      res <- sourcesService.addSource(dto).fold(
        v => BadRequestResponse(v.errors.mkString(", ")),
        x => SourceResponse(x)
      )
//      _ <- Task.fromFunction(
        // TODO ????
//        stream.publish(SourcesKeeperActor.NewSource(x))
//      )
    } yield res
  }

  def updateSource(sourceId: Long, dto: UpdateSourceDto): Z = {
    for {
      res <- sourcesService.updateSource(sourceId, dto).fold(
        v => BadRequestResponse(v.errors.mkString(", ")),
        x => SourceResponse(x)
      )
      // TODO
      // stream.publish(SourcesKeeperActor.ReloadSource(x))
    } yield res
  }


}