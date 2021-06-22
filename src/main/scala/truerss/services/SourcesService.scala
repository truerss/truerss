package truerss.services

import io.truerss.actorika._
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
                     val system: ActorSystem,
                     val sourceValidator: SourceValidator
                    ) {

  import FeedSourceDtoModelImplicits._

  def getAllForOpml: Task[Iterable[SourceViewDto]] = {
    dbLayer.sourceDao.all.map { sources =>
      sources.flatMap { source =>
        source.id.map { sourceId =>
          source.toView(sourceId)
        }
      }
    }
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
      val errorsMap = errors.map(x => x.sourceId -> x.errorsCount).toMap
      sources.flatMap { source =>
        source.id.map { sourceId =>
          source.toView(sourceId)
            .recount(feedsBySource.getOrElse(sourceId, 0))
            .errors(errorsMap.getOrElse(sourceId, 0))
        }
      }
    }
  }

  def getSource(sourceId: Long): Task[SourceViewDto] = {
    fetchOne(sourceId) { _.toView(sourceId) }
  }

  def delete(sourceId: Long): Task[Unit] = {
    for {
      source <- dbLayer.sourceDao.findOne(sourceId)
      view = source.toView(sourceId)
      _ <- dbLayer.sourceDao.delete(sourceId)
      _ <- dbLayer.feedDao.deleteFeedsBySource(sourceId)
      _ <- dbLayer.sourceStatusesDao.delete(sourceId)
      _ <- IO.effect { system.publish(SourcesKeeperActor.SourceDeleted(view)) }
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
            IO.effect {
              system.publish(WebSocketController.NotifyMessage(
                Notify(errors.mkString(", "), NotifyLevel.Warning)
              ))
            }
          case ex: Throwable =>
            IO.effect {
              system.publish(WebSocketController.NotifyMessage(
                Notify(ex.getMessage, NotifyLevel.Warning)
              ))
            }
        },
          validSource => {
            IO.effect {
              system.publish(EventHandlerActor.NewSourceCreated(validSource))
            }
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
      resultSource = newSource.withId(id).toView(id)
      _ <- dbLayer.sourceStatusesDao.insertOne(id)
      _ <- IO.effect {
        system.publish(SourcesKeeperActor.NewSource(resultSource))
      }
    } yield resultSource
  }

  def updateSource(sourceId: Long, dto: UpdateSourceDto): Task[SourceViewDto] = {
    for {
      _ <- sourceValidator.validateSource(dto)
      source = dto.toSource
      state = appPluginService.getState(source.url)
      updatedSource = source.withState(state).withId(sourceId)
      _ <- dbLayer.sourceDao.updateSource(updatedSource).orDie
      view = updatedSource.toView(sourceId)
      _ <- IO.effect { system.publish(SourcesKeeperActor.ReloadSource(view)) }
    } yield view
  }

  def changeLastUpdateTime(sourceId: Long): Task[Int] = {
    dbLayer.sourceDao.updateLastUpdateDate(sourceId)
  }

  private def fetchOne(sourceId: Long)(f: Source => SourceViewDto): Task[SourceViewDto] = {
    dbLayer.sourceDao.findOne(sourceId).map(f)
  }

}
