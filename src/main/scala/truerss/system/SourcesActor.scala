package truerss.system

import akka.actor._
import akka.actor.SupervisorStrategy.Restart
import akka.pattern.ask
import akka.util.Timeout
import truerss.controllers.ModelsResponse

import scala.concurrent.duration._

import truerss.models.Source
/**
 * Created by mike on 9.8.15.
 */
class SourcesActor(proxyRef: ActorRef) extends Actor with ActorLogging {

  import db.GetAll
  import network._
  import util.Start
  import context.dispatcher
  implicit val timeout = Timeout(30 seconds)

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
      sources.xs.foreach { source =>
        context.actorOf(Props(new SourceActor(source)), s"source-${source.normalize}")
      }
    }
  }


}
