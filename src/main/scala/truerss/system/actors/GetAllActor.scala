package truerss.system.actors

import akka.actor._
import truerss.api.ModelsResponse
import truerss.system.db

class GetAllActor(override val dbRef: ActorRef) extends CommonActor {

  import db._

  var source2feedCount: Map[Long, Int] = Map.empty

  def defaultHandler = {
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
      finish

  }

}

object GetAllActor {
  def props(dbRef: ActorRef) = Props(classOf[GetAllActor], dbRef)
}
