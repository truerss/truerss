package truerss.system

import akka.actor.{ActorLogging, Actor}

import com.github.truerss.base._

import java.util.Date

import truerss.models.Source

import scala.concurrent.duration._
import scala.util.{Right, Left}

class SourceActor(source: Source, feedReader: BaseFeedReader,
                   contentReaders: Vector[BaseContentReader
                     with UrlMatcher with Priority with PluginInfo])
  extends Actor with ActorLogging {

  import network._
  import util._
  import db.AddFeeds
  import truerss.util.Util.EntryExt
  import context.dispatcher

  val stream = context.system.eventStream

  val currentTime = new Date().getTime
  val lastUpdate = source.lastUpdate.getTime
  val interval = source.interval * 60 // interval in hours
  val diff = (currentTime - lastUpdate) / (60 * 1000)

  val tickTime = if ((diff > interval) || diff == 0) {
    0 seconds
  } else {
    (interval - diff) minutes
  }

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
          log.warning(s"Error when update source ${error}")
          stream.publish(Notify(NotifyLevels.Danger, error.error))
      }
      context.parent ! Updated


    case ExtractContent(sourceId, feedId, url) =>
      val c = contentReaders.filter(_.matchUrl(url)).sortBy(_.priority).head
      log.info(s"Read content from $url ~> (${source.name}) with ${c.pluginName}")
      c.content(url) match {
        case Right(content) =>
          sender ! ExtractContentForEntry(sourceId, feedId, content)
        case Left(error) =>
          sender ! ExtractError(error.error)
      }
  }

}
