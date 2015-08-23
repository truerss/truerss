package truerss.system

import akka.actor.{ActorLogging, Actor}

import java.util.Date

import truerss.models.Source

import scala.concurrent.duration._
/**
 * Created by mike on 9.8.15.
 */
class SourceActor(source: Source) extends Actor with ActorLogging {

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

  log.info(s"Next time update for ${source.name} -> ${tickTime} minutes")

  context.system.scheduler.schedule(
    tickTime,
    source.interval minutes,
    self,
    Update
  )

  def receive = {
    case Update =>
      log.info(s"Update ${source.normalized}")
      context.parent ! Grep(source.id.get, source.url)
  }


}
