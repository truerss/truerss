package truerss.services

import truerss.db.{DbLayer, Source}
import truerss.db.validation.SourceValidator
import truerss.dto.{ApplicationPlugins, NewSourceDto, SourceDto, SourceViewDto, UpdateSourceDto}
import truerss.services.management.FeedSourceDtoModelImplicits
import truerss.util.Util

import zio._

import scala.concurrent.{ExecutionContext, Future}

class SourcesService(dbLayer: DbLayer,
                     val appPlugins: ApplicationPlugins)(implicit ec: ExecutionContext) {

  import FeedSourceDtoModelImplicits._
  import Util._

  protected val sourceValidator = new SourceValidator(appPlugins)(dbLayer, ec)

  def getAllForOpml: Future[Vector[SourceViewDto]] = {
    dbLayer.sourceDao.all.map { xs => xs.map(_.toView).toVector }
  }

  def getAll: Task[Vector[SourceViewDto]] = {
    val feedsF = dbLayer.feedDao
      .feedBySourceCount(false)
      .map(_.toVector.toMap)
    val sourcesF = dbLayer.sourceDao.all.map(_.toVector)
    val result = for {
      feedsBySource <- feedsF
      sources <- sourcesF
    } yield {
      sources.map { s =>
        s.toView.recount(feedsBySource.getOrElse(s.id.get, 0))
      }
    }
    ft(result)
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
    val t = sourceValidator.validateSources(dtos).flatMap { valid =>
      val sources = valid.map { x =>
        x.toSource.withState(appPlugins.getState(x.url))
      }
      dbLayer.sourceDao.insertMany(sources).flatMap { _ =>
        dbLayer.sourceDao.fetchByUrls(sources.map(_.url))
      }
    }.map { xs => xs.map(_.toView) }
    ft(t)
  }

  //
  def addSource(dto: NewSourceDto): IO[ValidationError, SourceViewDto] = {
    for {
      _ <- sourceValidator.validateSource(dto)
      source = dto.toSource
      state = appPlugins.getState(source.url)
      newSource = source.withState(state)
      id <- ft(dbLayer.sourceDao.insert(newSource)).orDie
    } yield newSource.withId(id).toView
  }

  def updateSource(sourceId: Long, dto: UpdateSourceDto): IO[ValidationError, SourceViewDto] = {
    for {
      _ <- sourceValidator.validateSource(dto)
      source = dto.toSource
      state = appPlugins.getState(source.url)
      updatedSource = source.withState(state).withId(sourceId)
      _ <- ft(dbLayer.sourceDao.updateSource(updatedSource)).orDie
    } yield updatedSource.toView
  }

  def changeLastUpdateTime(sourceId: Long): Task[Int] = {
    ft(dbLayer.sourceDao.updateLastUpdateDate(sourceId))
  }

  private def fetchOne(sourceId: Long)(f: Source => SourceViewDto): Task[Option[SourceViewDto]] = {
    ft(dbLayer.sourceDao.findOne(sourceId).map { x => x.map(f) })
  }

  private def ft[T](x: Future[T]): Task[T] = {
    Task.fromFuture { implicit ec => x}
  }

}
