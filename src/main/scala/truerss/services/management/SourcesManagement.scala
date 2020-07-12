package truerss.services.management

import akka.event.EventStream
import truerss.api.{BadRequestResponse, NotFoundResponse, Response, SourceOverViewResponse, SourceResponse, SourcesResponse}
import truerss.dto.{NewSourceDto, UpdateSourceDto}
import truerss.services.{OpmlService, SourceOverviewService, SourcesService, ValidationError}
import truerss.services.actors.sync.SourcesKeeperActor
import zio.Task

import scala.concurrent.{ExecutionContext, Future}

class SourcesManagement(sourcesService: SourcesService,
                        opmlService: OpmlService,
                        sourceOverviewService: SourceOverviewService,
                        stream: EventStream
                       )
                       (implicit ec: ExecutionContext) extends BaseManagement {

  import ResponseHelpers.{sourceNotFound, ok}

  def all: Z = {
    sourcesService.getAll.map(SourcesResponse)
  }

  def getSource(sourceId: Long): Z = {
    sourcesService.getSource(sourceId).map {
      case Some(s) => SourceResponse(s)
      case _ => NotFoundResponse(s"Source $sourceId was not found")
    }
  }

  def getSourceOverview(sourceId: Long): Z = {
    sourceOverviewService.getSourceOverview(sourceId)
      .map(SourceOverViewResponse)
  }

  def markSource(sourceId: Long): Z = {
    sourcesService.markAsRead(sourceId).map(_ => ok)
  }

  def forceRefreshSource(sourceId: Long): Z = {
    stream.publish(SourcesKeeperActor.UpdateOne(sourceId))
    Task.effectTotal(ok)
  }

  def forceRefresh: Z = {
    stream.publish(SourcesKeeperActor.Update)
    Task.effectTotal(ok)
  }

  def deleteSource(sourceId: Long): Z = {
    sourcesService.delete(sourceId).map {
      case Some(x) =>
        stream.publish(SourcesKeeperActor.SourceDeleted(x))
        ok
      case _ => sourceNotFound
    }
  }

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