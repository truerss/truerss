package truerss.system

import akka.actor.{ActorLogging, Actor}

import com.github.truerss.base._

import java.net.URL
import java.util.Date

import org.jsoup.Jsoup

import truerss.models.Source

import scala.concurrent.duration._
import scala.util.{Right, Left}
import scala.util.control.Exception._

class SourceActor(source: Source, feedReader: BaseFeedReader,
                   contentReaders: Vector[aliases.WithContent])
  extends Actor with ActorLogging {

  import network._
  import util._
  import db.AddFeeds
  import truerss.util.Request._
  import truerss.util.Util.EntryExt
  import context.dispatcher

  val stream = context.system.eventStream

  val currentTime = new Date().getTime
  val lastUpdate = source.lastUpdate.getTime
  val interval = source.interval * 60 // interval in hours
  val diff = (currentTime - lastUpdate) / (60 * 1000)

//  val tickTime = if ((diff > interval) || diff == 0) {
//    0 seconds
//  } else {
//    (interval - diff) minutes
//  }
  val tickTime = 3 seconds

  log.info(s"Next time update for ${source.name} -> ${tickTime}; " +
    s"Interval: ${interval} minutes")

  context.system.scheduler.schedule(
    tickTime,
    interval minutes,
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

  private def extractContent(url: String) = {
    val response = getResponse(url)

    if (response.isError) {
      throw new RuntimeException(s"Connection error for $url")
    }

    Jsoup.parse(response.body).body().html()
  }

}
