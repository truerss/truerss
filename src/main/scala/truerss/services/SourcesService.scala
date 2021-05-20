package truerss.services

import akka.event.EventStream
import truerss.db.{DbLayer, Source}
import truerss.db.validation.SourceValidator
import truerss.dto.{NewSourceDto, SourceViewDto, UpdateSourceDto}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.util.FeedSourceDtoModelImplicits
import truerss.api.ws.{Notify, NotifyLevel, WebSocketController}
import truerss.services.actors.events.EventHandlerActor
import zio._

class SourcesService(val dbLayer: DbLayer,
                     val appPluginService: ApplicationPluginsService,
                     override val stream: EventStream,
                     val sourceValidator: SourceValidator
                    ) extends StreamProvider {

  import FeedSourceDtoModelImplicits._

  def getAllForOpml: Task[Vector[SourceViewDto]] = {
    dbLayer.sourceDao.all.map { xs => xs.map(_.toView).toVector }
  }

  def findAll: Task[Vector[SourceViewDto]] = {
    // TODO use join please
    for {
      feedsBySource <- dbLayer.feedDao
        .feedBySourceCount(false)
        .map(_.toVector.toMap)
      sources <- dbLayer.sourceDao.all.map(_.toVector)
      errors <- dbLayer.sourceStatusesDao.all
    } yield {
      val errorsMap = errors.map(x => x.sourceId -> x.sourceId).toMap
      sources.flatMap { source =>
        source.id.map { sourceId =>
          source.toView
            .recount(feedsBySource.getOrElse(sourceId, 0))
            .errors(errorsMap.getOrElse(sourceId, 0L))
        }
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
      _ <- dbLayer.sourceStatusesDao.delete(sourceId)
      _ <- fire(SourcesKeeperActor.SourceDeleted(view))
    } yield ()
  }

  // opml
  def addSources(dtos: Iterable[NewSourceDto]): Task[Unit] = {
    // todo use settings instead of constant
    Task.collectAllParN_(10) {
      dtos.map { dto =>
        addSource(dto).foldM(
        {
          case ValidationError(errors) =>
            fire(WebSocketController.NotifyMessage(
              Notify(errors.mkString(", "), NotifyLevel.Warning)
            ))
          case ex: Throwable =>
            fire(WebSocketController.NotifyMessage(
              Notify(ex.getMessage, NotifyLevel.Warning)
            ))
        },
          validSource => {
            fire(
              EventHandlerActor.NewSourceCreated(validSource)
            )
          }
        )
      }
    }
  }

  def addSource(dto: NewSourceDto): Task[SourceViewDto] = {
    for {
      _ <- sourceValidator.validateSource(dto)
      source = dto.toSource
      state = appPluginService.getState(source.url)
      newSource = source.withState(state)
      id <- dbLayer.sourceDao.insert(newSource).orDie
      resultSource = newSource.withId(id).toView
      _ <- dbLayer.sourceStatusesDao.insertOne(id)
      _ <- fire(SourcesKeeperActor.NewSource(resultSource))
    } yield resultSource
  }

  def updateSource(sourceId: Long, dto: UpdateSourceDto): Task[SourceViewDto] = {
    for {
      _ <- sourceValidator.validateSource(dto)
      source = dto.toSource
      state = appPluginService.getState(source.url)
      updatedSource = source.withState(state).withId(sourceId)
      _ <- dbLayer.sourceDao.updateSource(updatedSource).orDie
      view = updatedSource.toView
      _ <- fire(SourcesKeeperActor.ReloadSource(view))
    } yield view
  }

  def changeLastUpdateTime(sourceId: Long): Task[Int] = {
    dbLayer.sourceDao.updateLastUpdateDate(sourceId)
  }

  private def fetchOne(sourceId: Long)(f: Source => SourceViewDto): Task[SourceViewDto] = {
    dbLayer.sourceDao.findOne(sourceId).map(f)
  }

}
