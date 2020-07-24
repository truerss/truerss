package truerss.services

import akka.event.EventStream
import truerss.db.{DbLayer, Source}
import truerss.db.validation.SourceValidator
import truerss.dto.{ApplicationPlugins, NewSourceDto, SourceViewDto, UpdateSourceDto}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.util.{Util, FeedSourceDtoModelImplicits, EventStreamExt}
import zio._

class SourcesService(private val dbLayer: DbLayer,
                     private val appPlugins: ApplicationPlugins,
                     private val stream: EventStream,
                     private val sourceValidator: SourceValidator
                    ) {

  import FeedSourceDtoModelImplicits._
  import Util._
  import EventStreamExt._

  def getAllForOpml: Task[Vector[SourceViewDto]] = {
    dbLayer.sourceDao.all.map { xs => xs.map(_.toView).toVector }
  }

  def findAll: Task[Vector[SourceViewDto]] = {
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

  def delete(sourceId: Long): Task[Unit] = {
    for {
      source <- dbLayer.sourceDao.findOne(sourceId)
      view = source.toView
      _ <- dbLayer.sourceDao.delete(sourceId)
      _ <- dbLayer.feedDao.deleteFeedsBySource(sourceId)
      _ <- stream.fire(SourcesKeeperActor.SourceDeleted(view))
    } yield ()
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
      _ <- stream.fire(SourcesKeeperActor.NewSources(sources.map(_.toView)))
    } yield sources.map(_.toView)
  }

  def addSource(dto: NewSourceDto): Task[SourceViewDto] = {
    for {
      _ <- sourceValidator.validateSource(dto)
      source = dto.toSource
      state = appPlugins.getState(source.url)
      newSource = source.withState(state)
      id <- dbLayer.sourceDao.insert(newSource).orDie
      resultSource = newSource.withId(id).toView
      _ <- stream.fire(SourcesKeeperActor.NewSource(resultSource))
    } yield resultSource
  }

  def updateSource(sourceId: Long, dto: UpdateSourceDto): Task[SourceViewDto] = {
    for {
      _ <- sourceValidator.validateSource(dto)
      source = dto.toSource
      state = appPlugins.getState(source.url)
      updatedSource = source.withState(state).withId(sourceId)
      _ <- dbLayer.sourceDao.updateSource(updatedSource).orDie
      view = updatedSource.toView
      _ <- stream.fire(SourcesKeeperActor.ReloadSource(view))
    } yield view
  }

  def changeLastUpdateTime(sourceId: Long): Task[Int] = {
    dbLayer.sourceDao.updateLastUpdateDate(sourceId)
  }

  private def fetchOne(sourceId: Long)(f: Source => SourceViewDto): Task[SourceViewDto] = {
    dbLayer.sourceDao.findOne(sourceId).map(f)
  }

}
