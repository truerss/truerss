package truerss.system.actors

import akka.actor._
import truerss.system.db
import truerss.controllers.ModelsResponse

class LatestFavoritesActor(override val dbRef: ActorRef) extends CommonActor {

  import db.{Favorites, Latest, ResponseFeeds}

  def receive = {
    case msg @ (_: Latest | _ : Favorites.type) =>
      originalSender = sender
      dbRef ! msg

    case ResponseFeeds(xs) =>
      originalSender ! ModelsResponse(xs)
      context.stop(self)
  }

}

object LatestFavoritesActor {
  def props(dbRef: ActorRef) = Props(classOf[LatestFavoritesActor], dbRef)
}
