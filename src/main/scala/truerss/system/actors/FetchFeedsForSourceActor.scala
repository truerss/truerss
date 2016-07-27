package truerss.system.actors

import akka.actor._
import truerss.controllers.ModelsResponse
import truerss.models.Feed
import truerss.system.db

class FetchFeedsForSourceActor(override val dbRef: ActorRef) extends CommonActor {

  import db._

  var feeds: Vector[Feed] = Vector.empty
  var sourceId: Long = 0L

  def receive = {
    case msg: ExtractFeedsForSource =>
      originalSender = sender
      sourceId = msg.sourceId
      dbRef ! msg

    case ResponseFeeds(xs) =>
      feeds = xs
      dbRef ! FeedCountForSource(sourceId)

    case ResponseCount(count) =>
      ModelsResponse(feeds, count)
      context.stop(self)

  }

}

object FetchFeedsForSourceActor {
  def props(dbRef: ActorRef) = Props(classOf[FetchFeedsForSourceActor], dbRef)
}
