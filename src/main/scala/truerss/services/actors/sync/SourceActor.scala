package truerss.services.actors.sync

import java.time.ZoneOffset
import java.util.Date

import akka.actor.{Actor, ActorLogging, Props}
import com.github.truerss.base._
import org.jsoup.Jsoup
import truerss.dto.{Notify, NotifyLevels, SourceViewDto}
import truerss.services.DbHelperActor.{AddFeeds, SourceLastUpdate}
import truerss.services.actors.sync.SourcesKeeperActor.{Update, UpdateMe, Updated}

import scala.concurrent.duration._

class SourceActor(source: SourceViewDto, feedReader: BaseFeedReader)
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
      stream.publish(SourceLastUpdate(source.id))
      feedReader.newEntries(source.url) match {
        case Right(xs) =>
          stream.publish(AddFeeds(source.id, xs))
        case Left(error) =>
          log.warning(s"Error when update source $error")
          stream.publish(Notify(NotifyLevels.Danger, error.error))
      }
      context.parent ! Updated
  }


}

object SourceActor {
  import truerss.util.Request._

  def props(source: SourceViewDto, feedReader: BaseFeedReader) = {
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

  def extractContent(url: String): String = {
    val response = getResponse(url)

    if (response.isError) {
      throw new RuntimeException(s"Connection error for $url")
    }

    Jsoup.parse(response.body).body().html()
  }
}