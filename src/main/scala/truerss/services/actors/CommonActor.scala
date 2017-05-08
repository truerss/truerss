package truerss.services.actors

import akka.actor._
import truerss.util.Util.ResponseHelpers

import scala.concurrent.duration._

trait CommonActor extends
  Actor with ActorLogging with ResponseHelpers {
  var originalSender: ActorRef = null
  val stream = context.system.eventStream

  import context.dispatcher

  def stopInterval: FiniteDuration = 30 seconds

  val scheduler = context.system.scheduler.scheduleOnce(stopInterval) {
    Option(context).foreach(_.stop(self))
  }

  def defaultHandler: Receive

  override def receive = defaultHandler

  override def unhandled(message: Any): Unit = {
    log.warning(s"Unhandled message from $sender: $message")
  }

}

object CommonActor {
  case object Suicide
}
