package truerss.services.actors.management

import akka.actor.Props
import akka.pattern.pipe
import truerss.api._
import truerss.dto.{NewSourceDto, NewSourceFromFileWithErrors, UpdateSourceDto}
import truerss.services.SourcesService
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.util.Util.ResponseHelpers

import scala.concurrent.Future

/**
  * Created by mike on 4.5.17.
  */
class SourcesManagementActor(sourcesService: SourcesService) extends CommonActor {

  import SourcesManagementActor._
  import ResponseHelpers._
  import context.dispatcher

  override def receive: Receive = {
    case GetAll =>
      sourcesService.getAll.map(SourcesResponse) pipeTo sender

    case GetSource(sourceId) =>
      sourcesService.getSource(sourceId).map( x => SourceResponse(x.get)) pipeTo sender

    case Mark(sourceId) =>
      sourcesService.markAsRead(sourceId).map(_ => ok) pipeTo sender

    case DeleteSource(sourceId) =>
      sourcesService.delete(sourceId).map {
        case Some(x) =>
          stream.publish(SourcesKeeperActor.SourceDeleted(x))
          ok
        case _ => sourceNotFound
      } pipeTo sender

    case AddSource(dto) =>
      sourcesService.addSource(dto).map {
        case Left(errors) =>
          BadRequestResponse(errors.mkString(", "))

        case Right(x) =>
          stream.publish(SourcesKeeperActor.NewSource(x))
          SourceResponse(x)
      } pipeTo sender

    case UpdateSource(sourceId, dto) =>
      sourcesService.updateSource(sourceId, dto).map {
        case Left(errors) =>
          BadRequestResponse(errors.mkString(", "))

        case Right(x) =>
          stream.publish(SourcesKeeperActor.ReloadSource(x))
          SourceResponse(x)
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