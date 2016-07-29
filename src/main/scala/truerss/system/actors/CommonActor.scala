package truerss.system.actors

import akka.actor._
import truerss.controllers.InternalServerErrorResponse
import truerss.util.Util.ResponseHelpers

import scala.concurrent.duration._

trait CommonActor extends
  Actor with ActorLogging with ResponseHelpers {
  def dbRef: ActorRef
  var originalSender: ActorRef = null
  val stream = context.system.eventStream

  import context.dispatcher

  context.system.scheduler.scheduleOnce(10 seconds) {
    self ! CommonActor.Suicide
  }

  def handleSuicide: Receive = {
    case CommonActor.Suicide =>
      Option(originalSender).foreach(_ ! InternalServerErrorResponse("Timeout"))
      context.stop(self)
  }

  def defaultHandler: Receive

  override def receive = defaultHandler orElse handleSuicide

}

object CommonActor {
  case object Suicide
}