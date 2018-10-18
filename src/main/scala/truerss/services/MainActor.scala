package truerss.services

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import akka.event.EventStream
import truerss.db.DbLayer
import truerss.models.{Notify, NotifyLevels}
import truerss.util.TrueRSSConfig

import scala.concurrent.duration._

class MainActor(config: TrueRSSConfig,
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

  val stream: EventStream = context.system.eventStream


  val dbHelperActorRef = context.actorOf(
    DbHelperActor.props(dbLayer),
    "db-helper-actor")

  val sourcesRef = context.actorOf(SourcesActor.props(
    SourcesActor.SourcesSettings(config),
    dbLayer
  ), "sources-root-actor")

  val apiActorRef = context.actorOf(
    ApiActor.props(config.appPlugins, sourcesRef, dbLayer), "api-service-router")

  val publishActor = context.actorOf(Props(
    classOf[PublishPluginActor], config.appPlugins.publishPlugins),
    "publish-plugin-actor")

  stream.subscribe(publishActor, classOf[PublishPluginActor.PublishEvent])
  stream.subscribe(dbHelperActorRef, classOf[DbHelperActor.DbHelperActorMessage])
  stream.subscribe(sourcesRef, classOf[SourcesActor.NewSource])
  stream.subscribe(sourcesRef, classOf[SourcesActor.ReloadSource])

  def receive = {
    case x => apiActorRef forward x
  }

}

object MainActor {
  def props(config: TrueRSSConfig,
            dbLayer: DbLayer) = {
    Props(classOf[MainActor], config, dbLayer)
  }
}
