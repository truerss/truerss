package truerss.services

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import akka.event.EventStream
import truerss.db.DbLayer
import truerss.dto.{Notify, NotifyLevels}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.util.TrueRSSConfig

import scala.concurrent.duration._

class MainActor(config: TrueRSSConfig,
                applicationPluginsService: ApplicationPluginsService,
                sourcesService: SourcesService,
                dbLayer: DbLayer)
  extends Actor with ActorLogging {

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case x: java.sql.SQLException =>
        log.error(x, x.getMessage)
        context.system.eventStream.publish(Notify(NotifyLevels.Danger,
          "Db Error. System will be stopped"))
        Stop
      case x: Throwable =>
        log.error(x, x.getMessage)
        Resume
  }

  private val stream: EventStream = context.system.eventStream

  val dbHelperActorRef = context.actorOf(
    DbHelperActor.props(dbLayer),
    "db-helper-actor")

  val sourcesRef = context.actorOf(SourcesKeeperActor.props(
    SourcesKeeperActor.SourcesSettings(config),
    applicationPluginsService,
    sourcesService
  ), "sources-root-actor")

  val publishActor = context.actorOf(Props(
    classOf[PublishPluginActor], config.appPlugins.publishPlugins),
    "publish-plugin-actor")

  stream.subscribe(publishActor, classOf[PublishPluginActor.PublishEvent])
  stream.subscribe(dbHelperActorRef, classOf[DbHelperActor.DbHelperActorMessage])
  stream.subscribe(sourcesRef, classOf[SourcesKeeperActor.NewSource])
  stream.subscribe(sourcesRef, classOf[SourcesKeeperActor.SourceDeleted])
  stream.subscribe(sourcesRef, classOf[SourcesKeeperActor.ReloadSource])


  def receive = {
    case x =>
      log.warning(s"Unhandled message: $x, from: $sender")
  }

  private def create(props: Props): ActorRef = {
    context.actorOf(props.withDispatcher("dispatchers.truerss-dispatcher"))
  }

}

object MainActor {
  def props(config: TrueRSSConfig,
            applicationPluginsService: ApplicationPluginsService,
            sourcesService: SourcesService,
            dbLayer: DbLayer) = {
    Props(classOf[MainActor], config, applicationPluginsService,
      sourcesService,
      dbLayer)
  }
}
