package truerss.system

import akka.actor.{Actor, ActorLogging, Props, OneForOneStrategy}
import akka.actor.SupervisorStrategy._
import akka.io.IO
import akka.routing.SmallestMailboxPool
import akka.pattern.gracefulStop

import spray.can.Http

import truerss.api.{RoutingService, WSApi}
import truerss.config.TrueRSSConfig
import truerss.db.{SupportedDb, DbActor}
import truerss.models.CurrentDriver
import truerss.system.util.{NotifyLevels, Notify}

import slick.jdbc.JdbcBackend.DatabaseDef
import scala.concurrent.duration._
import scala.concurrent.Future

class SystemActor(config: TrueRSSConfig,
                  dbDef: DatabaseDef,
                  driver: CurrentDriver,
                  backend: SupportedDb) extends Actor with ActorLogging {

  import global._
  import context.dispatcher

  implicit val system = context.system

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case x: java.sql.SQLException =>
        log.error(x, x.getMessage)
        system.eventStream.publish(Notify(NotifyLevels.Danger,
          "Db Error. System stop"))
        self ! StopSystem
        Stop
      case x: Throwable =>
        log.error(x, x.getMessage)
        Resume
  }

  val dbRef = context.actorOf(
    Props(classOf[DbActor], dbDef, driver).withDispatcher("dispatchers.db-dispatcher"), "db")

  val sourcesRef = context.actorOf(Props(classOf[SourcesActor],
    config,
    self), "sources")

  val proxyRef = context.actorOf(Props(
    classOf[ProxyServiceActor],
      config.appPlugins, dbRef, sourcesRef, self
    ), "service-router")

  val api = context.actorOf(Props(classOf[RoutingService],
    proxyRef, config.wsPort,
    config.appPlugins.js.toVector,
    config.appPlugins.css.toVector), "api")

  val socketApi = context.actorOf(Props(classOf[WSApi], config.wsPort), "ws-api")

  IO(Http) ! Http.Bind(api, interface = config.host, port = config.port)

  def stopChildren = Future.sequence(Vector(
    dbRef, sourcesRef, proxyRef, api, socketApi
  ).map(gracefulStop(_, 10 seconds)))

  def receive = {
    case StopApp =>
      log.info(s"Stop application")
      system.registerOnTermination({
        sys.exit(1)
      })
      stopChildren.map(_ => system.terminate())

    case StopSystem =>
      log.info("Stop actor system")
      stopChildren.map(_ => system.terminate())

    case Http.Bound(isa) =>
      log.info(s"Run API on ${isa.getPort} port")

    case x => proxyRef forward x
  }

}
