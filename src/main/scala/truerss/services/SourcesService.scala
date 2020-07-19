package truerss.services

import truerss.db.{DbLayer, Source}
import truerss.db.validation.SourceValidator
import truerss.dto.{ApplicationPlugins, NewSourceDto, SourceViewDto, UpdateSourceDto}
import truerss.services.management.FeedSourceDtoModelImplicits
import truerss.util.Util
import zio._

import scala.concurrent.{ExecutionContext, Future}

class SourcesService(dbLayer: DbLayer,
                     val appPlugins: ApplicationPlugins)(implicit ec: ExecutionContext) {

  import FeedSourceDtoModelImplicits._
  import Util._

  protected val sourceValidator = new SourceValidator(appPlugins)(dbLayer)

  def getAllForOpml: Task[Vector[SourceViewDto]] = {
    dbLayer.sourceDao.all.map { xs => xs.map(_.toView).toVector }
  }

  def getAll: Task[Vector[SourceViewDto]] = {
    // TODO join
    for {
      feedsBySource <- dbLayer.feedDao
        .feedBySourceCount(false)
        .map(_.toVector.toMap)
      sources <- dbLayer.sourceDao.all.map(_.toVector)
    } yield sources.map { s =>
      s.toView.recount(feedsBySource.getOrElse(s.id.get, 0))
    }
  }

  def getSource(sourceId: Long): Task[Option[SourceViewDto]] = {
    fetchOne(sourceId) { _.toView }
  }

  def markAsRead(sourceId: Long): Task[Option[SourceViewDto]] = {
    fetchOne(sourceId) { source =>
      dbLayer.feedDao.markBySource(sourceId)
      source.toView
    }
  }

  def delete(sourceId: Long): Task[Option[SourceViewDto]] = {
    fetchOne(sourceId) { source =>
      dbLayer.sourceDao.delete(sourceId)
      dbLayer.feedDao.deleteFeedsBySource(sourceId)
      source.toView
    }
  }

  // opml
  def addSources(dtos: Iterable[NewSourceDto]): Task[Iterable[SourceViewDto]] = {
    for {
      valid <- sourceValidator.filterValid(dtos)
      sources = valid.map { x =>
        x.toSource.withState(appPlugins.getState(x.url))
      }
      _ <- dbLayer.sourceDao.insertMany(sources)
      sources <- dbLayer.sourceDao.fetchByUrls(sources.map(_.url).toSeq)
    } yield sources.map(_.toView)
  }

  //
  def addSource(dto: NewSourceDto): IO[ValidationError, SourceViewDto] = {
    for {
      _ <- sourceValidator.validateSource(dto)
      source = dto.toSource
      state = appPlugins.getState(source.url)
      newSource = source.withState(state)
      id <- dbLayer.sourceDao.insert(newSource).orDie
    } yield newSource.withId(id).toView
  }

  def updateSource(sourceId: Long, dto: UpdateSourceDto): IO[ValidationError, SourceViewDto] = {
    for {
      _ <- sourceValidator.validateSource(dto)
      source = dto.toSource
      state = appPlugins.getState(source.url)
      updatedSource = source.withState(state).withId(sourceId)
      _ <- dbLayer.sourceDao.updateSource(updatedSource).orDie
    } yield updatedSource.toView
  }

  def changeLastUpdateTime(sourceId: Long): Task[Int] = {
    dbLayer.sourceDao.updateLastUpdateDate(sourceId)
  }

  private def fetchOne(sourceId: Long)(f: Source => SourceViewDto): Task[Option[SourceViewDto]] = {
    dbLayer.sourceDao.findOne(sourceId).map { x => x.map(f) }
  }

}
