package truerss.system.actors

import akka.actor._
import truerss.system.db

class MarkMessagesActor(override val dbRef: ActorRef) extends CommonActor {

  import db._

  def defaultHandler = {
    case msg @ (_ : UnmarkFeed |
                _ : MarkAsReadFeed | _ : MarkAsUnreadFeed) =>
      originalSender = sender
      dbRef ! msg

    case ResponseMaybeFeed(maybeFeed) =>
      originalSender ! optionFeedResponse(maybeFeed)
      finish
  }

}

object MarkMessagesActor {
  def props(dbRef: ActorRef) = Props(classOf[MarkMessagesActor], dbRef)
}
