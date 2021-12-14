package truerss.services.actors.sync

import io.truerss.actorika._
import org.slf4j.LoggerFactory
import truerss.api.ws.{Notify, NotifyLevel, WebSocketController}
import truerss.dto.SourceViewDto
import truerss.services.ApplicationPluginsService
import truerss.services.actors.events.EventHandlerActor
import truerss.services.actors.sync.SourcesKeeperActor.{Update, UpdateMe, Updated}

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.duration._

class SourceActor(source: SourceViewDto, appPluginsService: ApplicationPluginsService)
  extends Actor {

  import SourceActor._
  import util._
  import ActorDsl._

  private val logger = LoggerFactory.getLogger(getClass)

  val updTime = UpdateTime(source)

  logger.info(s"Next time update for ${source.name} -> ${updTime.tickTime}; " +
    s"Interval: ${updTime.interval}")

  override def preStart(): Unit = {
    scheduler.every(updTime.interval, updTime.tickTime) {() =>
      parent ! UpdateMe(me)
    }
  }


  def receive = {
    case Update =>
      logger.info(s"Update ${source.normalized} -> ${source.url}")
      appPluginsService.getSourceReader(source).newEntries(source.url) match {
        case Right(xs) =>
          system.publish(EventHandlerActor.RegisterNewFeeds(source.id, xs))
        case Left(error) =>
          logger.warn(s"Error when update source $error")
          system.publish(WebSocketController.NotifySourceError(
            sourceId = source.id,
            message = Notify(error.error, NotifyLevel.Danger)
          ))
          system.publish(EventHandlerActor.SourceError(source.id))
      }

      parent ! Updated
      system.publish(EventHandlerActor.ModifySource(source.id))
  }


}

object SourceActor {
  def props(source: SourceViewDto,
            appPluginsService: ApplicationPluginsService): SourceActor = {
    new SourceActor(source, appPluginsService)
  }

  case class UpdateTime(
                         tickTime: FiniteDuration,
                         interval: FiniteDuration
                       )

  object UpdateTime {
    def apply(source: SourceViewDto): UpdateTime = {
      val interval = source.interval
      val lastUpdate = source.lastUpdate
        .plusHours(interval)
        .toInstant(ZoneOffset.UTC)
      val now = Instant.now(Clock.systemUTC())
      val tmp = java.time.Duration.between(now, lastUpdate)
      val between = if (tmp.isNegative) {
        0L
      } else {
        tmp.getSeconds
      }

      UpdateTime(
        tickTime = between seconds,
        interval = interval hours
      )
    }
  }

}