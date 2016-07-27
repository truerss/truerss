package truerss.system.actors

import akka.actor._
import truerss.controllers.OkResponse
import truerss.system.db

class MarkAllActor(override val dbRef: ActorRef) extends CommonActor {

  import db.{MarkAll, ResponseDone}

  def receive = {
    case MarkAll =>
      originalSender = sender
      dbRef ! MarkAll

    case ResponseDone(id) =>
      originalSender ! OkResponse(s"$id")
      context.stop(self)
  }

}

object MarkAllActor {
  def props(dbRef: ActorRef) = Props(classOf[MarkAllActor], dbRef)
}
