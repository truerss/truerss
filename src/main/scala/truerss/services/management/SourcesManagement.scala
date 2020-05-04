package truerss.services.management

import akka.event.EventStream
import truerss.api.{BadRequestResponse, NotFoundResponse, SourceOverViewResponse, SourceResponse, SourcesResponse}
import truerss.dto.{NewSourceDto, UpdateSourceDto}
import truerss.services.{OpmlService, SourceOverviewService, SourcesService}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.util.Util.ResponseHelpers.sourceNotFound

import scala.concurrent.{ExecutionContext, Future}

class SourcesManagement(sourcesService: SourcesService,
                        opmlService: OpmlService,
                        sourceOverviewService: SourceOverviewService,
                        stream: EventStream
                       )
                       (implicit ec: ExecutionContext) extends BaseManagement {

  def all: R = {
    sourcesService.getAll.map(SourcesResponse)
  }

  def getSource(sourceId: Long): R = {
    sourcesService.getSource(sourceId).map {
      case Some(s) => SourceResponse(s)
      case _ => NotFoundResponse(s"Source $sourceId was not found")
    }
  }

  def getSourceOverview(sourceId: Long): R = {
    sourceOverviewService.getSourceOverview(sourceId)
      .map(SourceOverViewResponse)
  }

  def markSource(sourceId: Long): R = {
    sourcesService.markAsRead(sourceId).map(_ => ok)
  }

  def forceRefreshSource(sourceId: Long): R = {
    stream.publish(SourcesKeeperActor.UpdateOne(sourceId))
    Future.successful(ok)
  }

  def forceRefresh: R = {
    stream.publish(SourcesKeeperActor.Update)
    Future.successful(ok)
  }

  def deleteSource(sourceId: Long): R = {
    sourcesService.delete(sourceId).map {
      case Some(x) =>
        stream.publish(SourcesKeeperActor.SourceDeleted(x))
        ok
      case _ => sourceNotFound
    }
  }

  def addSource(dto: NewSourceDto): R = {
    sourcesService.addSource(dto).map {
      case Left(errors) =>
        BadRequestResponse(errors.mkString(", "))

      case Right(x) =>
        stream.publish(SourcesKeeperActor.NewSource(x))
        SourceResponse(x)
    }
  }

  def updateSource(sourceId: Long, dto: UpdateSourceDto): R = {
    sourcesService.updateSource(sourceId, dto).map {
      case Left(errors) =>
        BadRequestResponse(errors.mkString(", "))

      case Right(x) =>
        stream.publish(SourcesKeeperActor.ReloadSource(x))
        SourceResponse(x)
    }
  }


}