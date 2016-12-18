package truerss.system.actors

import akka.actor._
import truerss.api.ModelsResponse
import truerss.system.{db, util}

class UnreadActor(override val dbRef: ActorRef) extends CommonActor {

  import db.ResponseFeeds
  import util.Unread

  def defaultHandler = {
    case msg: Unread =>
      originalSender = sender
      dbRef ! msg

    case ResponseFeeds(xs) =>
      originalSender ! ModelsResponse(xs)
      finish

  }

}

object UnreadActor {
  def props(dbRef: ActorRef) = Props(classOf[UnreadActor], dbRef)
}
