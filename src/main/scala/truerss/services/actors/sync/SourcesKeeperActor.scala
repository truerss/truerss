package truerss.services.actors.sync

import io.truerss.actorika.{ActorStrategies, _}
import org.slf4j.{Logger, LoggerFactory}
import truerss.dto.SourceViewDto
import truerss.services.{ApplicationPluginsService, SourcesService}

import scala.concurrent.duration._

class SourcesKeeperActor(config: SourcesKeeperActor.SourcesSettings,
                         appPluginService: ApplicationPluginsService,
                         sourcesService: SourcesService
                  ) extends Actor {

  import SourcesKeeperActor._
  import ActorDsl._

  private val ticker = new Ticker[ActorRef](config.parallelFeedUpdate)

  private val logger = LoggerFactory.getLogger(getClass)

  override def applyRestartStrategy(ex: Throwable, failedMessage: Option[Any], count: Int): ActorStrategies.Value = {
    logger.warn(s"exception in source actor: $ex")
    ActorStrategies.Skip
  }

  logger.info(s"Feed parallelism: ${config.parallelFeedUpdate}")

  override def preStart(): Unit = {
    implicit val ec = system.context
    zio.Runtime.default.unsafeRunToFuture(sourcesService.getAllForOpml)
      .map(Sources).foreach { result =>
      me ! result
    }
  }

  def uninitialized: Receive = {
    case Sources(xs) =>
      logger.info("Start actor per source")
      xs.filter(_.isEnabled).foreach(startSourceActor)
      become(initialized)
  }

  def initialized: Receive = {
    case Sources(xs) =>
      logger.info("Start actor per source")
      xs.filter(_.isEnabled).foreach(startSourceActor)

    case NewSource(source) =>
      startSourceActor(source)

    case Update =>
      logger.info(s"Update for ${children.size} actors")
      children.foreach { _ ! Update }

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
      logger.info(s"Stop ${source.name} actor")
      ticker.deleteOne(source.id).foreach { ref =>
        stop(ref)
      }

    case ReloadSource(source) =>
      ticker.deleteOne(source.id).foreach { ref =>
        stop(ref)
      }
      startSourceActor(source)
  }

  def receive: Receive = uninitialized


  private def startSourceActor(source: SourceViewDto): Unit = {
    val props = SourceActor.props(source, appPluginService)
    val ref = spawn(props, s"actor-${source.id}")
    ticker.addOne(source.id, ref)
  }

  private def nextTick = {
    if (ticker.nonEmpty) {
      scheduler.once(defaultDelay) { () =>
        me ! Tick
      }
    }
  }
}

object SourcesKeeperActor {

  protected val logger: Logger = LoggerFactory.getLogger(getClass)

  val defaultDelay: FiniteDuration = 15 seconds

  def props(config: SourcesSettings,
            appPluginService: ApplicationPluginsService,
            sourcesService: SourcesService
           ): SourcesKeeperActor = {
    new SourcesKeeperActor(config, appPluginService, sourcesService)
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
  case class Sources(xs: Iterable[SourceViewDto])

  // config
  case class SourcesSettings(
                            parallelFeedUpdate: Int
                            )


}