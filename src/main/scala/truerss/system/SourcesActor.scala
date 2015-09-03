package truerss.system

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.github.truerss.base.{BaseContentReader, BaseFeedReader}

import truerss.controllers._
import truerss.models.{Disable, Neutral, Source}
import truerss.plugins.DefaultSiteReader
import truerss.util.ApplicationPlugins

import scala.concurrent.duration._
import scala.collection.mutable.{ArrayBuffer, Map => MMap}
import scalaz.Scalaz._

class SourcesActor(plugins: ApplicationPlugins,
                   proxyRef: ActorRef,
                   networkRef: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher
  import db.{OnlySources, SetState}
  import network.{NetworkInitialize, NewSourceInfo, NetworkInitialized}
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
  var sources: ArrayBuffer[Source] = _

  case class AboutSource(sourceId: Long, feedReader: BaseFeedReader,
                          contentReader: BaseContentReader)

  def getSourceInfo(source: Source) = {
    source.state match {
      case Neutral =>
        AboutSource(source.id.get, defaultPlugin, defaultPlugin).some
      case _ =>
        val feedReader = plugins.getFeedReader(source.url)

        val contentReader = plugins.getContentReader(source.url)

        (feedReader, contentReader) match {
          case (None, None) =>
            log.warning(s"Disable ${source.id.get} -> ${source.name} Source. " +
              s"Plugin not found")
            sources -= source
            proxyRef ! SetState(source.id.get, Disable)
            none
          case (f, c) =>
            val f0 = f.getOrElse(defaultPlugin)
            val c0 = c.getOrElse(defaultPlugin)
            log.info(s"${source.name} need plugin." +
              s" Detect feed plugin: ${f0.pluginName}, " +
              s" content plugin: ${c0.pluginName}")
            AboutSource(source.id.get, f0, c0).some
        }

    }
  }


  def receive = {
    case Start =>
      (proxyRef ? OnlySources).mapTo[Vector[Source]].map { xs =>
        sources = xs.filter(_.state match {
          case Disable => false
          case _ => true
        }).to[ArrayBuffer]

        sourcesCount = sources.size
        log.info(s"Given ${sourcesCount} sources")

        val xs0 = xs.flatMap(getSourceInfo)
        val feedReaders = MMap.empty ++ xs0.map { about =>
          about.sourceId -> about.feedReader
        }.toMap
        val contentReaders = MMap.empty ++ xs0.map { about =>
          about.sourceId -> about.contentReader
        }.toMap
        networkRef ! NetworkInitialize(feedReaders, contentReaders)
      }

    case NetworkInitialized =>
      sources.foreach { source =>
        log.info(s"Start source actor for ${source.normalized} -> ${source.id.get}")
        context.actorOf(Props(new SourceActor(source, networkRef)),
          s"source-${source.id.get}")
      }

    case NewSource(source) =>
      getSourceInfo(source).map { about =>
        networkRef ! NewSourceInfo(about.sourceId, about.feedReader,
          about.contentReader)
        context.actorOf(Props(new SourceActor(source, networkRef)),
          s"source-${source.id.get}")
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
