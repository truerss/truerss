package truerss.services.actors.management

import akka.actor._

trait CommonActor extends
  Actor with ActorLogging {

  val stream = context.system.eventStream

  override def unhandled(message: Any): Unit = {
    log.warning(s"Unhandled message from $sender: $message")
  }

}

object CommonActor {
  case object Suicide
}
