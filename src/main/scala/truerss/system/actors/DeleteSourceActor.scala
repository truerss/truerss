package truerss.system.actors

import akka.actor.Actor.Receive
import akka.actor._
import truerss.controllers.ModelsResponse
import truerss.system.util.SourceDeleted
import truerss.system.{db, util}

class DeleteSourceActor(override val dbRef: ActorRef, sourcesRef: ActorRef) extends CommonActor {

  import db._

  /*
  (dbRef ? msg).mapTo[Option[Source]].map {
        case Some(source) =>
          sourcesRef ! SourceDeleted(source)
          stream.publish(SourceDeleted(source)) // => ws
          ok
        case None => sourceNotFound(msg)
      }
   */

  override def receive = {
    case msg: DeleteSource =>
      originalSender = sender
      dbRef ! msg

    case ResponseMaybeSource(mbSource) =>
      originalSender ! mbSource.map { source =>
        sourcesRef ! SourceDeleted(source) // TODO use publish
        stream.publish(SourceDeleted(source)) // => ws
        ok
      }.getOrElse(sourceNotFound)
      context.stop(self)
  }
}

object DeleteSourceActor {
  def props(dbRef: ActorRef, sourcesRef: ActorRef) =
    Props(classOf[DeleteSourceActor], dbRef, sourcesRef)
}