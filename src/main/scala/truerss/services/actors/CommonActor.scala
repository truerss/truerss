package truerss.services.actors

import akka.actor._
import truerss.util.Util.ResponseHelpers

trait CommonActor extends
  Actor with ActorLogging with ResponseHelpers {

  val stream = context.system.eventStream

  override def unhandled(message: Any): Unit = {
    log.warning(s"Unhandled message from $sender: $message")
  }

}

object CommonActor {
  case object Suicide
}
