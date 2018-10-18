package truerss.services.actors.sync

import java.net.URL

import akka.actor.SupervisorStrategy.Resume
import akka.actor._
import akka.event.EventStream
import akka.pattern.pipe
import akka.util.Timeout
import com.github.truerss.base._
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import truerss.db.DbLayer
import truerss.dto.SourceViewDto
import truerss.models._
import truerss.plugins.DefaultSiteReader
import truerss.services.{ApplicationPluginsService, SourcesService}
import truerss.util.{ApplicationPlugins, TrueRSSConfig}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._


class SourcesKeeperActor(config: SourcesKeeperActor.SourcesSettings,
                         appPluginService: ApplicationPluginsService,
                         sourcesService: SourcesService
                  ) extends Actor with ActorLogging {

  import SourcesKeeperActor._
  import context.dispatcher

  implicit val timeout = Timeout(30 seconds)
  val stream = context.system.eventStream

  override val supervisorStrategy = OneForOneStrategy() {
    case x: Throwable =>
      log.warning(s"exception in source actor: $x")
      Resume
  }

  private type CR = BaseContentReader with UrlMatcher with Priority with PluginInfo

  val queue = ArrayBuffer[ActorRef]()

  val maxUpdateCount = config.parallelFeedUpdate
  val sourceNetwork = scala.collection.mutable.Map[Long, ActorRef]()

  var inProgress = 0

  log.info(s"Feed parallelism: $maxUpdateCount")

  def nextTick = {
    if (queue.nonEmpty) {
      context.system.scheduler.scheduleOnce(15 seconds, self, Tick)
    }
  }

  override def preStart(): Unit = {
    sourcesService.getAllForOpml.map(Sources) pipeTo self
  }


  def uninitialized: Receive = {
    case Sources(xs) =>
      log.info("Start actor per source")
      xs.filter(_.state match {
        case Disable => false
        case _ => true
      }).foreach(startSourceActor)

      context.become(initialized)

    case any =>
      log.warning(s"Oops, something went wrong, when load sources from db: $any")
      // todo
  }


  def initialized: Receive = {
    case NewSource(source) =>
      startSourceActor(source)

    case Update =>
      log.info(s"Update for ${context.children.size} actors")
      context.children.foreach{ _ ! Update }

    case UpdateMe(ref) =>
      if (inProgress >= maxUpdateCount) {
        queue += ref
        nextTick
      } else {
        inProgress += 1
        ref ! Update
      }

    case Updated =>
      inProgress -= 1

    case Tick =>
      if (inProgress >= maxUpdateCount) {
        nextTick
      } else {
        queue.slice(0, maxUpdateCount).foreach { ref =>
          queue -= ref
          inProgress += 1
          ref ! Update
        }
      }

    case UpdateOne(num) =>
      sourceNetwork.get(num).foreach(_ ! Update)

    case SourceDeleted(source) =>
      log.info(s"Stop ${source.name} actor")
      sourceNetwork.get(source.id).foreach{ ref =>
        queue.filter(_ == ref).foreach(queue -= _)
        context.stop(ref)
      }
      sourceNetwork -= source.id

    case msg: SourceActor.ExtractContent =>
      sourceNetwork.get(msg.sourceId).foreach(_ forward msg)

    case ReloadSource(source) =>
      val id = source.id
      sourceNetwork.get(id).foreach(ref => context.stop(ref))
      sourceNetwork -= id

      startSourceActor(source)

    case x => log.warning(s"Unhandled message $x")
  }

  def receive = uninitialized


  private def startSourceActor(source: SourceViewDto) = {
    appPluginService.getSourceReader(source).map { feedReader =>
      log.info(s"Start source actor for ${source.normalized} -> ${source.id} with state ${source.state}")
      val ref = context.actorOf(Props(classOf[SourceActor],
        source, feedReader, appPluginService.contentReaders))
      sourceNetwork += source.id -> ref
    }
  }
}

object SourcesKeeperActor {

  val defaultPlugin = new DefaultSiteReader(ConfigFactory.empty())

  protected val logger = LoggerFactory.getLogger(getClass)

  def props(config: SourcesSettings,
            appPluginService: ApplicationPluginsService,
            sourcesService: SourcesService
           ): Props = {
    Props(classOf[SourcesKeeperActor], config, appPluginService, sourcesService)
  }

  sealed trait SourcesMessage

  case object Tick extends SourcesMessage
  case object Updated extends SourcesMessage
  case class UpdateMe(who: ActorRef) extends SourcesMessage
  case class SourceDeleted(source: SourceViewDto) extends SourcesMessage
  case object Update extends SourcesMessage
  case class UpdateOne(num: Long) extends SourcesMessage
  case class NewSource(source: SourceViewDto) extends SourcesMessage
  case class ReloadSource(source: SourceViewDto) extends SourcesMessage

  // not message, just tmp class with all sources
  case class Sources(xs: Seq[SourceViewDto])

  // config
  case class SourcesSettings(
                            appPlugins: ApplicationPlugins,
                            parallelFeedUpdate: Int
                            )
  object SourcesSettings {
    def apply(config: TrueRSSConfig): SourcesSettings = {
      SourcesSettings(
        appPlugins = config.appPlugins,
        parallelFeedUpdate = config.parallelFeedUpdate
      )
    }
  }

}