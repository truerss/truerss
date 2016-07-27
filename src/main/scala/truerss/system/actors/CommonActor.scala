package truerss.system.actors

import akka.actor._
import truerss.util.Util.responseHelpers

trait CommonActor extends Actor with responseHelpers {
  def dbRef: ActorRef
  var originalSender: ActorRef = null
  val stream = context.system.eventStream
}
