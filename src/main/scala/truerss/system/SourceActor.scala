package truerss.system

import akka.actor.{ActorRef, ActorLogging, Actor}

import java.util.Date

import truerss.models.Source

import scala.concurrent.duration._
/**
 * Created by mike on 9.8.15.
 */
class SourceActor(source: Source, networkRef: ActorRef)
  extends Actor with ActorLogging {

  import network._
  import util._
  import db.AddFeeds
  import truerss.util.Util.EntryExt
  import context.dispatcher

  val stream = context.system.eventStream

  val currentTime = (new Date()).getTime
  val lastUpdate = source.lastUpdate.getTime
  val interval = source.interval * 60 // interval in hours
  val diff = (currentTime - lastUpdate) / (60 * 1000)

  val tickTime = if ((diff > interval) || diff == 0) {
    0 seconds
  } else {
    (interval - diff) minutes
  }

  log.info(s"Next time update for ${source.name} -> ${tickTime}")

  context.system.scheduler.schedule(
    tickTime,
    source.interval minutes,
    self,
    Update
  )

  def receive = {
    case Update =>
      log.info(s"Update ${source.normalized}")
      stream.publish(SourceLastUpdate(source.id.get))
      networkRef ! Grep(source.id.get, source.url)

    case ExtractedEntries(sourceId, xs) =>
      stream.publish(AddFeeds(sourceId, xs.map(_.toFeed(sourceId))))
    case ExtractError(error) =>
      log.error(s"Error when update ${source.normalized} : ${error}")
    case SourceNotFound(sourceId) =>
      // todo restart system
      log.error(s"Source ${sourceId} not found")

  }


}
