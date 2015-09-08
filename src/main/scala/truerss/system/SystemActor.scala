package truerss.system

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.IO
import akka.routing.SmallestMailboxPool
import akka.pattern.gracefulStop

import spray.can.Http

import truerss.api.{RoutingService, WSApi}
import truerss.config.TrueRSSConfig
import truerss.db.DbActor
import truerss.models.CurrentDriver

import scala.slick.jdbc.JdbcBackend.DatabaseDef
import scala.concurrent.duration._
import scala.concurrent.Future

class SystemActor(config: TrueRSSConfig,
                  dbDef: DatabaseDef,
                  driver: CurrentDriver) extends Actor with ActorLogging {

  import global._
  import context.dispatcher
  implicit val system = context.system

  val dbRef = context.actorOf(Props(new DbActor(dbDef, driver)), "db")

  val sourcesRef = context.actorOf(Props(new SourcesActor(
    config.appPlugins,
    self)), "sources")

  val proxyRef = context.actorOf(Props(
    new ProxyServiceActor(config.appPlugins, dbRef, sourcesRef, self))
      .withRouter(SmallestMailboxPool(10)), "service-router")

  val api = context.actorOf(Props(new RoutingService(proxyRef, config.wsPort)), "api")

  val socketApi = context.actorOf(Props(new WSApi(config.wsPort)), "ws-api")

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
      stopChildren.map(_ => system.shutdown())

    case StopSystem =>
      log.info("Stop actor system")
      stopChildren.map(_ => system.shutdown())

    case x => proxyRef forward x
  }

}
