package truerss.services.actors

import akka.actor.Props
import akka.pattern.pipe
import truerss.api._
import truerss.dto.{NewSourceDto, UpdateSourceDto}
import truerss.services.{SourcesActor, SourcesService}

/**
  * Created by mike on 4.5.17.
  */
class SourcesManagementActor(sourcesService: SourcesService) extends CommonActor {

  import SourcesManagementActor._
  import context.dispatcher

  override def defaultHandler: Receive = {
    case GetAll =>
      sourcesService.getAll.map(SourcesResponse) pipeTo sender

    case GetSource(sourceId) =>
      sourcesService.getSource(sourceId).map(SourceResponse) pipeTo sender

    case Mark(sourceId) =>
      sourcesService.markAsRead(sourceId).map(_ => ok) pipeTo sender

    case DeleteSource(sourceId) =>
      sourcesService.delete(sourceId).map {
        case Some(x) =>
          stream.publish(WSController.SourceDeleted(x))
          stream.publish(SourcesActor.SourceDeleted(x))
          ok
        case _ => sourceNotFound
      } pipeTo sender

    case AddSource(dto) =>
      sourcesService.addSource(dto).map {
        case Left(errors) =>
          BadRequestResponse(errors.mkString(", "))

        case Right(x) =>
          stream.publish(WSController.SourceAdded(x))
          stream.publish(SourcesActor.NewSource(x))
          SourceResponse(Some(x))
      } pipeTo sender

    case UpdateSource(sourceId, dto) =>
      sourcesService.updateSource(sourceId, dto).map {
        case Left(errors) =>
          BadRequestResponse(errors.mkString(", "))

        case Right(x) =>
          stream.publish(WSController.SourceUpdated(x))
          stream.publish(SourcesActor.ReloadSource(x))
          SourceResponse(Some(x))
      } pipeTo sender

  }
}

object SourcesManagementActor {

  def props(sourcesService: SourcesService) = {
    Props(classOf[SourcesManagementActor], sourcesService)
  }

  sealed trait SourcesMessage
  case object GetAll extends SourcesMessage
  case class GetSource(sourceId: Long) extends SourcesMessage
  case class DeleteSource(sourceId: Long) extends SourcesMessage
  case class AddSource(source: NewSourceDto) extends SourcesMessage
  case class UpdateSource(sourceId: Long, source: UpdateSourceDto) extends SourcesMessage
  case class Mark(sourceId: Long) extends SourcesMessage

}