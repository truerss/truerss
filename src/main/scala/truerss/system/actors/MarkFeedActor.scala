package truerss.system.actors

import akka.actor._
import truerss.controllers
import truerss.system.{db, util}

class MarkFeedActor(override val dbRef: ActorRef) extends CommonActor {

  import db.{MarkFeed, ResponseMaybeFeed}
  import truerss.api.ModelResponse
  import util.PublishEvent

  def defaultHandler = {
    case msg: MarkFeed =>
      originalSender = sender
      dbRef ! msg

    case ResponseMaybeFeed(maybeFeed) =>
      originalSender ! maybeFeed.map { feed =>
        stream.publish(PublishEvent(feed))
        ModelResponse(feed)
      }.getOrElse(feedNotFound)
      context.stop(self)
  }

}

object MarkFeedActor {
  def props(dbRef: ActorRef) = Props(classOf[MarkFeedActor], dbRef)
}