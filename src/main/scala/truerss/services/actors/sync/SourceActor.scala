package truerss.services.actors.sync

import akka.actor.{Actor, ActorLogging, Props}
import truerss.api.ws.WebSocketController
import truerss.dto.{Notify, NotifyLevel, SourceViewDto}
import truerss.services.ApplicationPluginsService
import truerss.services.actors.events.EventHandlerActor
import truerss.services.actors.sync.SourcesKeeperActor.{Update, UpdateMe, Updated}

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.duration._

class SourceActor(source: SourceViewDto, appPluginsService: ApplicationPluginsService)
  extends Actor with ActorLogging {

  import SourceActor._
  import context.dispatcher
  import util._

  val stream = context.system.eventStream

  val updTime = UpdateTime(source)

  log.info(s"Next time update for ${source.name} -> ${updTime.tickTime}; " +
    s"Interval: ${updTime.interval}")

  context.system.scheduler.scheduleWithFixedDelay(
    updTime.tickTime,
    updTime.interval,
    context.parent,
    UpdateMe(self)
  )

  def receive = {
    case Update =>
      log.info(s"Update ${source.normalized} -> ${source.url}")
      appPluginsService.getSourceReader(source).newEntries(source.url) match {
        case Right(xs) =>
          stream.publish(EventHandlerActor.RegisterNewFeeds(source.id, xs))
        case Left(error) =>
          log.warning(s"Error when update source $error")
          stream.publish(WebSocketController.NotifyMessage(
            Notify(error.error, NotifyLevel.Danger)
          ))
      }

      context.parent ! Updated
      stream.publish(EventHandlerActor.ModifySource(source.id))
  }


}

object SourceActor {
  def props(source: SourceViewDto, appPluginsService: ApplicationPluginsService): Props = {
    Props(classOf[SourceActor], source, appPluginsService)
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