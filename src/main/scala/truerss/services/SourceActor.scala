package truerss.services

import java.net.URL
import java.util.Date

import akka.actor.{Actor, ActorLogging}
import com.github.truerss.base._
import org.jsoup.Jsoup
import truerss.models.{Notify, NotifyLevels, Source}
import truerss.services.DbHelperActor.{AddFeeds, SourceLastUpdate}
import truerss.services.SourcesActor._

import scala.concurrent.duration._
import scala.util.control.Exception._

class SourceActor(source: Source, feedReader: BaseFeedReader,
                   contentReaders: Vector[aliases.WithContent])
  extends Actor with ActorLogging {

  import SourceActor._
  import context.dispatcher

  import util._

  val stream = context.system.eventStream

  val updTime = UpdateTime(source.lastUpdate.getTime, source.interval)

  log.info(s"Next time update for ${source.name} -> ${updTime.tickTime}; " +
    s"Interval: ${updTime.interval}")

  context.system.scheduler.schedule(
    updTime.tickTime,
    updTime.interval,
    context.parent,
    UpdateMe(self)
  )

  def receive = {
    case Update =>
      log.info(s"Update ${source.normalized}")
      stream.publish(SourceLastUpdate(source.id.get))
      feedReader.newEntries(source.url) match {
        case Right(xs) =>
          stream.publish(AddFeeds(source.id.get, xs))
        case Left(error) =>
          log.warning(s"Error when update source $error")
          stream.publish(Notify(NotifyLevels.Danger, error.error))
      }
      context.parent ! Updated


    case ExtractContent(sourceId, feedId, url) =>
      val url0 = new URL(url)
      val c = contentReaders
        .filter(_.matchUrl(url0)).sortWith((a, b) => a.priority > b.priority).head
      log.info(s"Read content from $url ~> (${source.name}) with ${c.pluginName}")

      def pass(result: Either[Errors.Error, Option[String]]) = {
        result match {
          case Right(content) =>
            sender ! ExtractContentForEntry(sourceId, feedId, content)
          case Left(error) =>
            sender ! ExtractError(error.error)
        }
      }

      if (c.needUrl) {
        pass(c.content(ContentTypeParam.UrlRequest(url0)))
      } else {
        catching(classOf[Exception]) either extractContent(url) fold(
          err => {
            sender ! ExtractError(err.getMessage)
          },
          html => pass(c.content(ContentTypeParam.HtmlRequest(html)))
        )
      }

  }


}

object SourceActor {
  import truerss.util.Request._

  case class ExtractContent(sourceId: Long, feedId: Long, url: String)

  sealed trait NetworkResult

  case class ExtractedEntries(sourceId: Long, entries: Vector[Entry]) extends NetworkResult
  case class ExtractContentForEntry(sourceId: Long, feedId: Long, content: Option[String]) extends NetworkResult
  case class ExtractError(message: String) extends NetworkResult
  case class SourceNotFound(sourceId: Long) extends NetworkResult

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