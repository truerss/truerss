package truerss.system

import akka.actor.{ActorLogging, Actor}

import java.util.Date

import truerss.models.Source

import scala.concurrent.duration._
/**
 * Created by mike on 9.8.15.
 */
class SourceActor(source: Source) extends Actor  with ActorLogging {

  import network._
  import util._

  import context.dispatcher

  val currentTime = (new Date()).getTime
  val lastUpdate = source.lastUpdate.getTime
  val interval = source.interval * 60 // interval in hours
  val diff = (currentTime - lastUpdate) / (60 * 1000)

  val tickTime = if ((diff > interval) || diff == 0) {
    0 minutes
  } else {
    (interval - diff) minutes
  }

  log.debug(s"Next time update for ${source.name} -> ${tickTime} minutes")

  context.system.scheduler.schedule(
    tickTime,
    source.interval minutes,
    self,
    Update
  )

  def receive = {
    case Update =>
      // update lastUpdate time in db
      // use plugin
      context.parent ! Grep(source.url)
  }


}
