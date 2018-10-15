package truerss.services.actors

import akka.actor.Props
import akka.pattern.pipe
import truerss.api._
import truerss.db.DbLayer
import truerss.dto.{NewSourceDto, UpdateSourceDto}
import truerss.services.SourcesActor
import truerss.util.{ApplicationPlugins, SourceValidator, Util}

import scala.concurrent.Future

/**
  * Created by mike on 4.5.17.
  */
class SourcesManagementActor(dbLayer: DbLayer, appPlugins: ApplicationPlugins) extends CommonActor {

  import DtoModelImplicits._
  import SourcesManagementActor._
  import Util._
  import context.dispatcher

  override def defaultHandler: Receive = {
    case GetAll =>
      val result = for {
        feedsBySource <- dbLayer.feedDao.feedBySourceCount(false)
          .map(_.toVector.toMap)
        sources <- dbLayer.sourceDao.all.map(_.toVector)
      } yield {
        sources.map { s =>
          s.recount(feedsBySource.getOrElse(s.id.get, 0))
        }
      }

      result.map(ModelsResponse(_)) pipeTo sender

    case GetSource(sourceId) =>
      val result = dbLayer.sourceDao.findOne(sourceId).map {
        case Some(source) =>
          ModelResponse(source)
        case None =>
          sourceNotFound
      }
      result pipeTo sender


    case Mark(sourceId) =>
      val result = dbLayer.sourceDao.findOne(sourceId).map { source =>
        dbLayer.feedDao.markBySource(sourceId)
        source
      }.map { _ => ok }

      result pipeTo sender


    case DeleteSource(sourceId) =>
      val result = dbLayer.sourceDao.findOne(sourceId).map {
        case Some(source) =>
          dbLayer.sourceDao.delete(sourceId)
          dbLayer.feedDao.deleteFeedsBySource(sourceId)
          stream.publish(WSController.SourceDeleted(source))
          stream.publish(SourcesActor.SourceDeleted(source))
          ok
        case None =>
          sourceNotFound
      }

      result pipeTo sender

    case AddSource(dto) =>
      val result = SourceValidator.validateSource(dto, dbLayer).flatMap {
        case Right(_) =>
          val source = dto.toSource
          val state = appPlugins.getState(source.url)
          val newSource = source.withState(state)
          dbLayer.sourceDao.insert(newSource).map { id =>
            stream.publish(WSController.SourceAdded(newSource.withId(id)))
            stream.publish(SourcesActor.NewSource(newSource.withId(id)))
            ModelResponse(newSource.withId(id))
          }

        case Left(errors) =>
          Future.successful(BadRequestResponse(errors.mkString(", ")))
      }

      result pipeTo sender

    case UpdateSource(sourceId, dto) =>
      val result = SourceValidator
        .validateSource(dto, dbLayer).flatMap {
        case Right(_) =>
          val source = dto.toSource.withId(sourceId)
          val state = appPlugins.getState(source.url)
          val updatedSource = source.withState(state).withId(sourceId)
          dbLayer.sourceDao.updateSource(updatedSource).map { _ =>
            stream.publish(WSController.SourceUpdated(updatedSource))
            stream.publish(SourcesActor.ReloadSource(updatedSource))
            ModelResponse(updatedSource)
          }

        case Left(errors) =>
          Future.successful(BadRequestResponse(errors.mkString(", ")))
      }

      result pipeTo sender


  }
}

object SourcesManagementActor {

  def props(dbLayer: DbLayer, appPlugins: ApplicationPlugins) = {
    Props(classOf[SourcesManagementActor], dbLayer, appPlugins)
  }

  sealed trait SourcesMessage
  case object GetAll extends SourcesMessage
  case class GetSource(sourceId: Long) extends SourcesMessage
  case class DeleteSource(sourceId: Long) extends SourcesMessage
  case class AddSource(source: NewSourceDto) extends SourcesMessage
  case class UpdateSource(sourceId: Long, source: UpdateSourceDto) extends SourcesMessage
  case class Mark(sourceId: Long) extends SourcesMessage

}