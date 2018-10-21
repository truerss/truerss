package truerss.services

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import akka.event.EventStream
import truerss.db.DbLayer
import truerss.dto.{Notify, NotifyLevels}
import truerss.services.actors.management._
import truerss.services.actors.sync.{SourceActor, SourcesKeeperActor}
import truerss.util.TrueRSSConfig
import truerss.util.Util.ResponseHelpers

import scala.concurrent.duration._

class MainActor(config: TrueRSSConfig,
                dbLayer: DbLayer)
  extends Actor with ActorLogging {

  import context.dispatcher

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

  private val sourcesService = new SourcesService(dbLayer, config.appPlugins)
  private val applicationPluginsService = new ApplicationPluginsService(config.appPlugins)
  private val opmlService = new OpmlService(sourcesService)
  private val feedsService = new FeedsService(dbLayer)

  private val sourcesManagementActor = create(SourcesManagementActor.props(sourcesService))
  private val feedsManagementActor = create(FeedsManagementActor.props(feedsService))
  private val opmlActor = create(OpmlActor.props(opmlService))
  private val pluginManagementActor = create(PluginManagementActor.props(config.appPlugins))

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
  stream.subscribe(sourcesRef, classOf[SourcesKeeperActor.ReloadSource])


  def receive = {
    case msg: SourcesManagementActor.SourcesMessage =>
      sourcesManagementActor forward msg

    case msg: FeedsManagementActor.FeedsMessage =>
      feedsManagementActor forward msg

    case msg: OpmlActor.OpmlActorMessage =>
      opmlActor forward msg

    case msg: PluginManagementActor.PluginManagementMessage =>
      pluginManagementActor forward msg

    case msg: SourceActor.ExtractContent =>
      sourcesRef forward msg

      // todo remove
    case msg: SourcesKeeperActor.SourcesMessage =>
      stream.publish(msg)
      sender ! ResponseHelpers.ok
  }

  private def create(props: Props): ActorRef = {
    context.actorOf(props.withDispatcher("dispatchers.truerss-dispatcher"))
  }

}

object MainActor {
  def props(config: TrueRSSConfig,
            dbLayer: DbLayer) = {
    Props(classOf[MainActor], config, dbLayer)
  }
}
