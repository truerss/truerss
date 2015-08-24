package truerss.system

import akka.actor._
import akka.actor.SupervisorStrategy.Restart
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import truerss.controllers._
import truerss.plugins.DefaultSiteReader


import scala.concurrent.duration._

import truerss.models.Source
/**
 * Created by mike on 9.8.15.
 */
class SourcesActor(proxyRef: ActorRef, networkRef: ActorRef) extends Actor with ActorLogging {

  import db.OnlySources
  import network.{NetworkInitialize, SourceInfo}
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

  context.system.scheduler.scheduleOnce(3 seconds, self, Start)


  def receive = {
    case Start =>
      val defaultPlugin = new DefaultSiteReader(Map.empty)
      (proxyRef ? OnlySources).mapTo[Vector[Source]].map { sources =>
        sourcesCount = sources.size
        log.info(s"Given ${sourcesCount} sources")
        val info = sources.map(x => SourceInfo(x.id.get, defaultPlugin))
        networkRef ! NetworkInitialize(info)
        sources.foreach { source =>
          log.info(s"Start source actor for ${source.normalized} -> ${source.id.get}")
          context.actorOf(Props(new SourceActor(source, networkRef)), s"source-${source.id.get}")
        }
    }

    case Update =>
      log.info(s"Update for ${context.children.size} actors")
      context.children.foreach{ _ ! Update }
      sender ! OkResponse("updated")

    case UpdateOne(num) =>
      log.info(s"Update source ${num}")
      context.actorSelection(s"source-${num}") ! Update

    case x => log.warning(s"Unhandled message ${x}")
  }


}
