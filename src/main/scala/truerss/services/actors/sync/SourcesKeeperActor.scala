package truerss.services.actors.sync

import akka.actor.SupervisorStrategy.Resume
import akka.actor._
import akka.pattern.pipe
import akka.util.Timeout
import org.slf4j.LoggerFactory
import truerss.db.SourceStates
import truerss.dto.SourceViewDto
import truerss.services.{ApplicationPluginsService, SourcesService}
import truerss.util.TrueRSSConfig

import scala.concurrent.duration._

class SourcesKeeperActor(config: SourcesKeeperActor.SourcesSettings,
                         appPluginService: ApplicationPluginsService,
                         sourcesService: SourcesService
                  ) extends Actor with ActorLogging {

  import SourcesKeeperActor._
  import context.dispatcher

  implicit val timeout = Timeout(30 seconds)

  val stream = context.system.eventStream
  val ticket = new Ticker[ActorRef](config.parallelFeedUpdate)

  override val supervisorStrategy = OneForOneStrategy() {
    case x: Throwable =>
      log.warning(s"exception in source actor: $x")
      Resume
  }

  log.info(s"Feed parallelism: ${config.parallelFeedUpdate}")

  def nextTick = {
    if (ticket.nonEmpty) {
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
        case SourceStates.Disable => false
        case _ => true
      }).foreach(startSourceActor)

      context.become(initialized)

    case any =>
      log.warning(s"Oops, something went wrong, when load sources from db: $any")
  }


  def initialized: Receive = {
    case NewSource(source) =>
      startSourceActor(source)

    case Update =>
      log.info(s"Update for ${context.children.size} actors")
      context.children.foreach{ _ ! Update }

    case UpdateMe(ref) =>
      ticket.push(ref) match {
        case Some(_) =>
          ref ! Update

        case None =>
          nextTick
      }

    case Updated =>
      ticket.down()

    case Tick =>
      ticket.pop() match {
        case Some(xs) =>
          xs.foreach(_ ! Update)

        case None =>
          nextTick
      }

    case UpdateOne(num) =>
      ticket.getOne(num).foreach(_ ! Update)

    case SourceDeleted(source) =>
      log.info(s"Stop ${source.name} actor")
      ticket.deleteOne(source.id).foreach { ref =>
        context.stop(ref)
      }

    case msg: SourceActor.ExtractContent =>
      ticket.getOne(msg.sourceId).foreach(_ forward msg)

    case ReloadSource(source) =>
      ticket.deleteOne(source.id).foreach { ref =>
        context.stop(ref)
      }
      startSourceActor(source)

    case x => log.warning(s"Unhandled message $x")
  }

  def receive: Receive = uninitialized


  private def startSourceActor(source: SourceViewDto) = {
    appPluginService.getSourceReader(source).foreach { feedReader =>
      log.info(s"Start source actor for ${source.normalized} -> ${source.id} with state ${source.state}")
      val props = SourceActor.props(source, feedReader, appPluginService.contentReaders)
      val ref = context.actorOf(props)
      ticket.addOne(source.id, ref)
    }
  }
}

object SourcesKeeperActor {

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
                            parallelFeedUpdate: Int
                            )
  object SourcesSettings {
    def apply(config: TrueRSSConfig): SourcesSettings = {
      SourcesSettings(
        parallelFeedUpdate = config.parallelFeedUpdate
      )
    }
  }

}