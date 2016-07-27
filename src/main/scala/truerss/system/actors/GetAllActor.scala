package truerss.system.actors

import akka.actor._
import truerss.controllers.ModelsResponse
import truerss.system.db

class GetAllActor(dbRef: ActorRef) extends Actor {

  import db._

  var originalSender: ActorRef = null
  var source2feedCount: Map[Long, Int] = Map.empty

  def receive = {
    case GetAll =>
      originalSender = sender
      dbRef ! FeedCount(false)

    case ResponseFeedCount(response) =>
      source2feedCount = response.toMap
      dbRef ! GetAll

    case ResponseSources(sources) =>
      val response = ModelsResponse(
        sources.map { s =>
          s.recount(source2feedCount.getOrElse(s.id.get, 0))
        }
      )

      originalSender ! response
      context.stop(self)

  }

}

object GetAllActor {
  def props(dbRef: ActorRef) = Props(classOf[GetAllActor], dbRef)
}
