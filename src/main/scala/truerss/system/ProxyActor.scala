package truerss.system

import akka.actor.{ActorRef, Actor}
import akka.util.Timeout
import akka.pattern._
import akka.event.LoggingReceive

import scala.language.postfixOps
import scala.concurrent.duration._
/**
 * Created by mike on 2.8.15.
 */
class ProxyActor(dbRef: ActorRef) extends Actor {

  import truerss.controllers.{ModelsResponse, ModelResponse, NotFoundResponse}
  import db._
  import truerss.models.{Source, Feed}
  import context.dispatcher

  implicit val timeout = Timeout(7 seconds)

  def sourceNotFound(num: Long) = NotFoundResponse(s"Source with id = ${num} not found")

  def receive = LoggingReceive {
    case GetAll => (dbRef ? GetAll).mapTo[Vector[Source]].map(ModelsResponse(_)) pipeTo sender

    case msg: GetSource => (dbRef ? msg).mapTo[Option[Source]].map{
      case Some(x) => ModelResponse(x)
      case None => sourceNotFound(msg.num)
    } pipeTo sender

    case msg: AddSource => (dbRef ? msg).mapTo[Long]
      .map{x => ModelResponse(msg.source.copy(id = Some(x)))} pipeTo sender

  }


}
