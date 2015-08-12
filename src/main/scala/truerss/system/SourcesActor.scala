package truerss.system

import akka.actor._
import akka.actor.SupervisorStrategy.Restart
import akka.pattern.ask
import akka.util.Timeout
import truerss.controllers._

import scala.concurrent.duration._

import truerss.models.Source
/**
 * Created by mike on 9.8.15.
 */
class SourcesActor(proxyRef: ActorRef) extends Actor with ActorLogging {

  import db.{GetAll, GetSource}
  import network._
  import util._
  import context.dispatcher
  implicit val timeout = Timeout(30 seconds)

  var sourcesCount: Long = 0
  var updated = 0
  val maxUpdateCount = 10

  override val supervisorStrategy = OneForOneStrategy(
      maxNrOfRetries = 3,
      withinTimeRange = 1 minute) {
    case x: Throwable =>
      log.error(x, x.getMessage)
      Restart
  }

  context.system.scheduler.scheduleOnce(30 seconds, self, Start)


  def receive = {
    case Start => (proxyRef ? GetAll).mapTo[ModelsResponse[Source]].map { sources =>
      sourcesCount = sources.xs.size
      sources.xs.foreach { source =>
        context.actorOf(Props(new SourceActor(source)), s"source-${source.normalize}")
      }
    }

    case Update =>
      context.children.foreach{ _ ! Update }
      sender ! OkResponse("updated")

    case UpdateOne(num) => //TODO use actorSelection and id insead of normalize
      val original = sender
      (proxyRef ? GetSource).mapTo[Response].map {
        case ModelResponse(x: Source) =>
          context.actorSelection(s"**/source-${x.normalize}") ! Update
          original ! ModelResponse(x)
        case NotFoundResponse(msg) =>
          log.debug(s"Not found source with id = ${num}")
          original ! NotFoundResponse(msg)
        case x =>
          log.warning(s"Unexpected message ${x}")
          original ! InternalServerErrorResponse("Unexpected error")
      }

    case x => context.parent forward x
  }


}
