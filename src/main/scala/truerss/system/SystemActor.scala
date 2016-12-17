package truerss.system

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import akka.http.scaladsl.Http
import akka.pattern.gracefulStop
import akka.stream.ActorMaterializer
import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.api.{RoutingApiImpl, RoutingService, WSApi}
import truerss.config.TrueRSSConfig
import truerss.db.{DbActor, SupportedDb}
import truerss.models.CurrentDriver
import truerss.system.util.{Notify, NotifyLevels}

import scala.concurrent.Future
import scala.concurrent.duration._

class SystemActor(config: TrueRSSConfig,
                  dbDef: DatabaseDef,
                  driver: CurrentDriver,
                  backend: SupportedDb)
  extends Actor with ActorLogging {

  import context.dispatcher
  import global._

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()

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

  Http().bindAndHandle(new RoutingApiImpl(proxyRef).route, config.host, config.port)

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

    case x => proxyRef forward x
  }

}
