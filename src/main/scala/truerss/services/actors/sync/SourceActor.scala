package truerss.services.actors.sync

import java.time.ZoneOffset
import java.util.Date

import akka.actor.{Actor, ActorLogging, Props}
import com.github.truerss.base._
import org.jsoup.Jsoup
import truerss.api.WebSockerController
import truerss.dto.{Notify, NotifyLevel, SourceViewDto}
import truerss.services.actors.events.EventHandlerActor
import truerss.services.actors.sync.SourcesKeeperActor.{Update, UpdateMe, Updated}

import scala.concurrent.duration._

class SourceActor(source: SourceViewDto,
                  feedReader: BaseFeedReader)
  extends Actor with ActorLogging {

  import SourceActor._
  import context.dispatcher

  import util._

  val stream = context.system.eventStream

  val updTime = UpdateTime(source.lastUpdate.toInstant(ZoneOffset.UTC).getEpochSecond, source.interval)

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
      log.info(s"Update ${source.normalized}")
      feedReader.newEntries(source.url) match {
        case Right(xs) =>
          stream.publish(EventHandlerActor.RegisterNewFeeds(source.id, xs))
        case Left(error) =>
          log.warning(s"Error when update source $error")
          stream.publish(WebSockerController.NotifyMessage(
            Notify(error.error, NotifyLevel.Danger)
          ))
      }
      context.parent ! Updated
      stream.publish(EventHandlerActor.ModifySource(source.id))
  }


}

object SourceActor {
  def props(source: SourceViewDto, feedReader: BaseFeedReader): Props = {
    Props(classOf[SourceActor], source, feedReader)
  }

  case class UpdateTime(
                         tickTime: FiniteDuration,
                         interval: FiniteDuration
                       )

  object UpdateTime {
    def apply(lastUpdate: Long, sourceInterval: Long): UpdateTime = {
      val currentTime = new Date().getTime
      val interval = sourceInterval * 60 // interval in hours
      val diff = (currentTime - lastUpdate) / (60 * 1000)

      val tickTime = if ((diff > interval) || diff == 0) {
        0 seconds
      } else {
        (interval - diff) minutes
      }
      UpdateTime(
        tickTime = tickTime,
        interval = interval minutes
      )
    }
  }

}