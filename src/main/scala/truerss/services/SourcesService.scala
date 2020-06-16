package truerss.services

import truerss.db.{DbLayer, Source}
import truerss.db.validation.SourceValidator
import truerss.dto.{NewSourceDto, SourceDto, SourceViewDto, UpdateSourceDto}
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
    fetchOne(sourceId) { _.toView }
  }

  def markAsRead(sourceId: Long): Future[Option[SourceViewDto]] = {
    fetchOne(sourceId) { source =>
      dbLayer.feedDao.markBySource(sourceId)
      source.toView
    }
  }

  def delete(sourceId: Long): Future[Option[SourceViewDto]] = {
    fetchOne(sourceId) { source =>
      dbLayer.sourceDao.delete(sourceId)
      dbLayer.feedDao.deleteFeedsBySource(sourceId)
      source.toView
    }
  }

  // opml
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

  //
  def addSource(dto: NewSourceDto): Future[Either[scala.List[String], SourceViewDto]] = {
    processBeforeUpsert(dto) {
      val source = dto.toSource
      val state = appPlugins.getState(source.url)
      val newSource = source.withState(state)
      dbLayer.sourceDao.insert(newSource).map { id =>
        newSource.withId(id).toView
      }
    }
  }

  def updateSource(sourceId: Long, dto: UpdateSourceDto): Future[Either[scala.List[String], SourceViewDto]] = {
    processBeforeUpsert(dto) {
      val source = dto.toSource.withId(sourceId)
      val state = appPlugins.getState(source.url)
      val updatedSource = source.withState(state).withId(sourceId)
      dbLayer.sourceDao.updateSource(updatedSource).map { _ =>
        updatedSource.toView
      }
    }
  }

  private def processBeforeUpsert(dto: SourceDto)(f: => Future[SourceViewDto]): Future[Either[scala.List[String], SourceViewDto]] = {
    sourceValidator.validateSource(dto).flatMap {
      case Right(_) =>
        f.map(Right(_))

      case Left(errors) =>
        Future.successful(Left(errors))
    }
  }

  private def fetchOne(sourceId: Long)(f: Source => SourceViewDto): Future[Option[SourceViewDto]] = {
    dbLayer.sourceDao.findOne(sourceId).map { x => x.map(f) }
  }

}
