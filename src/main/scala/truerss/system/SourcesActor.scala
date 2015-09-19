package truerss.system

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import akka.pattern.{ask, gracefulStop}
import akka.util.Timeout

import com.github.truerss.base.{PluginInfo, Priority, UrlMatcher, BaseContentReader}

import truerss.controllers._
import truerss.models.{Enable, Disable, Neutral, Source}
import truerss.plugins.DefaultSiteReader
import truerss.util.ApplicationPlugins

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.collection.mutable.ArrayBuffer
import scalaz.Scalaz._


class SourcesActor(plugins: ApplicationPlugins,
                   proxyRef: ActorRef) extends Actor with ActorLogging {

  import context.dispatcher
  import db.{OnlySources, SetState}
  import network.ExtractContent
  import global.RestartSystem
  import util._

  case object Tick

  implicit val timeout = Timeout(30 seconds)

  val defaultPlugin = new DefaultSiteReader(Map.empty)
  val contentReaders: Vector[BaseContentReader with UrlMatcher
    with Priority with PluginInfo] =
    plugins.contentPlugins.toVector ++ Vector(defaultPlugin)
  val queue = ArrayBuffer[ActorRef]()

  val maxUpdateCount = 10
  var currentUpdateTask = 0
  val sourceNetwork = scala.collection.mutable.Map[Long, ActorRef]()

  override val supervisorStrategy = OneForOneStrategy(
      maxNrOfRetries = 3,
      withinTimeRange = 1 minute) {
    case x: Throwable =>
      log.error(x, x.getMessage)
      Restart
  }

  context.system.scheduler.scheduleOnce(3 seconds, self, Start)

  def nextTick = {
    if (queue.size > 0) {
      context.system.scheduler.scheduleOnce(15 seconds, self, Tick)
    }
  }

  def getSourceReader(source: Source) = {
    source.state match {
      case Neutral =>
        defaultPlugin.some
      case Enable =>
        val feedReader = plugins.getFeedReader(source.url)

        val contentReader = plugins.getContentReader(source.url)

        (feedReader, contentReader) match {
          case (None, None) =>
            log.warning(s"Disable ${source.id.get} -> ${source.name} Source. " +
              s"Plugin not found")
            proxyRef ! SetState(source.id.get, Disable)
            none
          case (f, c) =>
            val f0 = f.getOrElse(defaultPlugin)
            val c0 = c.getOrElse(defaultPlugin)
            log.info(s"${source.name} need plugin." +
              s" Detect feed plugin: ${f0.pluginName}, " +
              s" content plugin: ${c0.pluginName}")
            f0.some
        }

      case Disable => none

    }
  }

  def startSourceActor(source: Source) = {
    getSourceReader(source).map { feedReader =>
      log.info(s"Start source actor for ${source.normalized} -> ${source.id.get}")
      val ref = context.actorOf(Props(classOf[SourceActor],
        source, feedReader, contentReaders))
      sourceNetwork += source.id.get -> ref
    }
  }

  def receive = {
    case Start =>
      log.info("Start source actors")
      (proxyRef ? OnlySources).mapTo[Vector[Source]].map { xs =>
        currentUpdateTask = 0
        sourceNetwork.clear()
        queue.clear()
        xs.filter(_.state match {
          case Disable => false
          case _ => true
        }).foreach(startSourceActor)
      }

    case NewSource(source) =>
      startSourceActor(source)

    case Update =>
      log.info(s"Update for ${context.children.size} actors")
      context.children.foreach{ _ ! Update }

    case UpdateMe(ref) =>
      if (currentUpdateTask >= maxUpdateCount) {
        queue += ref
        nextTick
      } else {
        currentUpdateTask += 1
        ref ! Update
      }

    case RestartSystem =>
      Future.sequence(sourceNetwork.values.map(gracefulStop(_, 10 seconds)))
        .map { x =>
          self ! Start
      }

    case Updated =>
      currentUpdateTask -= 1

    case Tick =>
      if (currentUpdateTask > maxUpdateCount) {
        nextTick
      } else {
        queue.slice(0, maxUpdateCount).foreach { ref =>
          queue -= ref
          ref ! Update
        }
      }

    case UpdateOne(num) =>
      sourceNetwork.get(num).foreach(_ ! Update)

    case SourceDeleted(source) =>
      log.info(s"Stop ${source.name} actor")
      sourceNetwork.get(source.id.get).foreach{ ref =>
        queue.filter(_ == ref).foreach(queue -= _)
        context.stop(ref)
      }
      sourceNetwork -= source.id.get

    case msg: ExtractContent =>
      sourceNetwork.get(msg.sourceId).foreach(_ forward msg)

    case x => log.warning(s"Unhandled message ${x}")
  }


}
