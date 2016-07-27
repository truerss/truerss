package truerss.system.actors

import akka.actor._
import truerss.controllers.ModelsResponse
import truerss.system.{db, util}

class UnreadActor(override val dbRef: ActorRef) extends CommonActor {

  import db.ResponseFeeds
  import util.Unread

  def receive = {
    case msg: Unread =>
      originalSender = sender
      dbRef ! msg

    case ResponseFeeds(xs) =>
      originalSender ! ModelsResponse(xs)
      context.stop(self)

  }

}

object UnreadActor {
  def props(dbRef: ActorRef) = Props(classOf[UnreadActor], dbRef)
}
