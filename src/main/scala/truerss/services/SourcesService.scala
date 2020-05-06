package truerss.services

import truerss.db.DbLayer
import truerss.db.validation.SourceValidator
import truerss.dto.{NewSourceDto, SourceViewDto, UpdateSourceDto}
import truerss.services.management.FeedSourceDtoModelImplicits
import truerss.util.{ApplicationPlugins, Util}

import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}

class SourcesService(dbLayer: DbLayer, val appPlugins: ApplicationPlugins)(implicit ec: ExecutionContext) {

  import FeedSourceDtoModelImplicits._
  import Util._

  private val logger = LoggerFactory.getLogger(getClass)

  protected val sourceValidator = new SourceValidator(appPlugins)(dbLayer, ec)

  def getAllForOpml: Future[Vector[SourceViewDto]] = {
    dbLayer.sourceDao.all.map { xs => xs.map(_.toView).toVector }
  }

  def getAll: Future[Vector[SourceViewDto]] = {
    val result = for {
      feedsBySource <- dbLayer.feedDao.feedBySourceCount(false)
        .map(_.toVector.toMap)
      sources <- dbLayer.sourceDao.all.map(_.toVector)
    } yield {
      sources.map { s =>
        s.toView.recount(feedsBySource.getOrElse(s.id.get, 0))
      }
    }
    result
  }

  def getSource(sourceId: Long): Future[Option[SourceViewDto]] = {
    dbLayer.sourceDao.findOne(sourceId).map {
      case Some(source) =>
        Some(source.toView)
      case None =>
        None
    }
  }

  def markAsRead(sourceId: Long): Future[Option[SourceViewDto]] = {
    dbLayer.sourceDao.findOne(sourceId).map { source =>
      dbLayer.feedDao.markBySource(sourceId)
      source.map(_.toView)
    }
  }

  def delete(sourceId: Long): Future[Option[SourceViewDto]] = {
    dbLayer.sourceDao.findOne(sourceId).map {
      case Some(source) =>
        dbLayer.sourceDao.delete(sourceId)
        dbLayer.feedDao.deleteFeedsBySource(sourceId)
        Some(source.toView)

      case None =>
        None
    }
  }

  def addSources(dtos: Iterable[NewSourceDto]): Future[Iterable[SourceViewDto]] = {
    sourceValidator.validateSources(dtos).flatMap { valid =>
      val sources = valid.map { x =>
        x.toSource.withState(appPlugins.getState(x.url))
      }
      dbLayer.sourceDao.insertMany(sources).flatMap { _ =>
        dbLayer.sourceDao.fetchByUrls(sources.map(_.url))
      }
    }.map { xs => xs.map(_.toView) }
  }

  def addSource(dto: NewSourceDto): Future[Either[scala.List[String], SourceViewDto]] = {
    sourceValidator.validateSource(dto).flatMap {
      case Right(_) =>
        val source = dto.toSource
        val state = appPlugins.getState(source.url)
        val newSource = source.withState(state)
        dbLayer.sourceDao.insert(newSource).map { id =>
          Right(newSource.withId(id).toView)
        }

      case Left(errors) =>
        Future.successful(Left(errors))
    }
  }

  def updateSource(sourceId: Long, dto: UpdateSourceDto): Future[Either[scala.List[String], SourceViewDto]] = {
    sourceValidator.validateSource(dto).flatMap {
      case Right(_) =>
        val source = dto.toSource.withId(sourceId)
        val state = appPlugins.getState(source.url)
        val updatedSource = source.withState(state).withId(sourceId)
        dbLayer.sourceDao.updateSource(updatedSource).map { _ =>
          Right(updatedSource.toView)
        }

      case Left(errors) =>
        Future.successful(Left(errors))
    }
  }

}
