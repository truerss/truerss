package truerss.services.actors.sync

import akka.actor.SupervisorStrategy.Resume
import akka.actor._
import akka.pattern.pipe
import org.slf4j.LoggerFactory
import truerss.dto.SourceViewDto
import truerss.services.{ApplicationPluginsService, SourcesService}

import scala.concurrent.duration._

class SourcesKeeperActor(config: SourcesKeeperActor.SourcesSettings,
                         appPluginService: ApplicationPluginsService,
                         sourcesService: SourcesService
                  ) extends Actor with ActorLogging {

  import SourcesKeeperActor._
  import context.dispatcher

  private val ticker = new Ticker[ActorRef](config.parallelFeedUpdate)

  override val supervisorStrategy = OneForOneStrategy() {
    case x: Throwable =>
      log.warning(s"exception in source actor: $x")
      Resume
  }

  log.info(s"Feed parallelism: ${config.parallelFeedUpdate}")

  override def preStart(): Unit = {
    sourcesService.getAllForOpml.map(Sources) pipeTo self
  }

  def uninitialized: Receive = {
    case Sources(xs) =>
      log.info("Start actor per source")
      xs.filter(_.isEnabled).foreach(startSourceActor)

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
      ticker.push(ref) match {
        case Some(_) =>
          ref ! Update

        case None =>
          nextTick
      }

    case Updated =>
      ticker.down()

    case Tick =>
      ticker.pop() match {
        case Some(xs) =>
          xs.foreach(_ ! Update)

        case None =>
          nextTick
      }

    case UpdateOne(num) =>
      ticker.getOne(num).foreach(_ ! Update)

    case SourceDeleted(source) =>
      log.info(s"Stop ${source.name} actor")
      ticker.deleteOne(source.id).foreach { ref =>
        context.stop(ref)
      }

    case ReloadSource(source) =>
      ticker.deleteOne(source.id).foreach { ref =>
        context.stop(ref)
      }
      startSourceActor(source)
  }


  def receive: Receive = uninitialized

  private def startSourceActor(source: SourceViewDto) = {
    val feedReader = appPluginService.getSourceReader(source)
    log.info(s"Start source actor for ${source.normalized} -> ${source.id}, state=${source.state}")
    val props = SourceActor.props(source, feedReader)
    val ref = context.actorOf(props)
    ticker.addOne(source.id, ref)
  }

  private def nextTick = {
    if (ticker.nonEmpty) {
      context.system.scheduler.scheduleOnce(defaultDelay, self, Tick)
    }
  }
}

object SourcesKeeperActor {

  protected val logger = LoggerFactory.getLogger(getClass)

  private val defaultDelay = 15 seconds

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


}