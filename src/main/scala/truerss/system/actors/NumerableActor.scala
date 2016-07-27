package truerss.system.actors

import akka.actor._
import truerss.controllers.ModelResponse
import truerss.system.db

class NumerableActor(override val dbRef: ActorRef) extends CommonActor {

  import db.{Numerable, ResponseMaybeSource}

  override def defaultHandler = {
    case msg: Numerable =>
      originalSender = sender
      dbRef ! msg

    case ResponseMaybeSource(maybeSource) =>
      originalSender ! maybeSource.map(ModelResponse(_))
        .getOrElse(sourceNotFound)
      context.stop(self)
  }
}

object NumerableActor {
  def props(dbRef: ActorRef) = Props(classOf[NumerableActor], dbRef)
}
