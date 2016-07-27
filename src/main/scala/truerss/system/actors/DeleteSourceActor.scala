package truerss.system.actors

import akka.actor._
import truerss.system.db
import truerss.system.util

class DeleteSourceActor(override val dbRef: ActorRef, sourcesRef: ActorRef) extends CommonActor {

  import db._
  import util.SourceDeleted

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