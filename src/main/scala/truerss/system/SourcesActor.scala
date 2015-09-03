package truerss.system

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.ActorDSL._

import truerss.controllers._
import truerss.models.{Neutral, Source}
import truerss.plugins.DefaultSiteReader
import truerss.util.ApplicationPlugins

import scala.concurrent.duration._


class SourcesActor(plugins: ApplicationPlugins,
                   proxyRef: ActorRef,
                   networkRef: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher
  import db.OnlySources
  import network.{NetworkInitialize, NewSourceInfo, SourceInfo, NetworkInitialized}
  import util._
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

  val defaultPlugin = new DefaultSiteReader(Map.empty)

  def getSourceInfo(source: Source) = {
    source.state match {
      case Neutral =>
        SourceInfo(source.id.get, defaultPlugin, defaultPlugin)
      case _ =>
        val feedReader = plugins.getFeedReader(source.url)
          .getOrElse(defaultPlugin)
        val contentReader = plugins.getContentReader(source.url)
          .getOrElse(defaultPlugin)

        log.info(s"${source.name} need plugin." +
          s" Detect feed plugin: ${feedReader.pluginName}, " +
          s" content plugin: ${contentReader.pluginName}")
        SourceInfo(source.id.get, feedReader, contentReader)
    }
  }

  var sources: Vector[Source] = _

  def receive = {
    case Start =>
      //TODO onlysource return only actual sources (neutral, enable state)
      (proxyRef ? OnlySources).mapTo[Vector[Source]].map { xs =>
        sourcesCount = xs.size
        log.info(s"Given ${sourcesCount} sources")
        sources = xs
        networkRef ! NetworkInitialize(xs.map(getSourceInfo))
      }

    case NetworkInitialized =>
      sources.foreach { source =>
        log.info(s"Start source actor for ${source.normalized} -> ${source.id.get}")
        context.actorOf(Props(new SourceActor(source, networkRef)),
          s"source-${source.id.get}")
      }

    case NewSource(source) =>
      networkRef ! NewSourceInfo(getSourceInfo(source))
      context.actorOf(Props(new SourceActor(source, networkRef)),
        s"source-${source.id.get}")

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
