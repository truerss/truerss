package truerss.system.actors

import akka.actor._
import truerss.api.OkResponse
import truerss.system.db

class MarkAllActor(override val dbRef: ActorRef) extends CommonActor {

  import db.{MarkAll, ResponseDone}

  def defaultHandler = {
    case MarkAll =>
      originalSender = sender
      dbRef ! MarkAll

    case ResponseDone(id) =>
      originalSender ! OkResponse(s"$id")
      finish
  }

}

object MarkAllActor {
  def props(dbRef: ActorRef) = Props(classOf[MarkAllActor], dbRef)
}
