package truerss.services

import akka.event.EventStream
import truerss.db.{DbLayer, Source}
import truerss.db.validation.SourceValidator
import truerss.dto.{ApplicationPlugins, NewSourceDto, SourceViewDto, UpdateSourceDto}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.services.management.FeedSourceDtoModelImplicits
import truerss.util.Util
import zio._

import scala.concurrent.{ExecutionContext}

class SourcesService(val dbLayer: DbLayer,
                     val appPlugins: ApplicationPlugins,
                     val stream: EventStream
                    )(implicit ec: ExecutionContext) {

  import FeedSourceDtoModelImplicits._
  import Util._

  protected val sourceValidator = new SourceValidator(appPlugins)(dbLayer)

  def refreshSource(sourceId: Int): Task[Unit] = {
    // todo in task
    stream.publish(SourcesKeeperActor.UpdateOne(sourceId))
    Task.effectTotal(())
  }

  def refreshAll: Task[Unit] = {
    stream.publish(SourcesKeeperActor.Update)
    Task.effectTotal(())
  }

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
    } yield {
      sources.map { s =>
        s.toView.recount(feedsBySource.getOrElse(s.id.get, 0))
      }
    }
  }

  def getSource(sourceId: Long): Task[SourceViewDto] = {
    fetchOne(sourceId) { _.toView }
  }

  def markAsRead(sourceId: Long): Task[Unit] = {
    fetchOne(sourceId) { source =>
      dbLayer.feedDao.markBySource(sourceId)
      source.toView
    }.map(_ => ())
  }

  def delete(sourceId: Long): Task[Unit] = {
    //  todo in for
    fetchOne(sourceId) { source =>
      val view = source.toView
      dbLayer.sourceDao.delete(sourceId)
      dbLayer.feedDao.deleteFeedsBySource(sourceId)
      stream.publish(SourcesKeeperActor.SourceDeleted(view))
      view
    }.map(_ => ())
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

  private def fetchOne(sourceId: Long)(f: Source => SourceViewDto): Task[SourceViewDto] = {
    dbLayer.sourceDao.findOne(sourceId).map(f)
  }

}
