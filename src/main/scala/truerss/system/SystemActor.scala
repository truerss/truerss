package truerss.system

import akka.actor.{Actor, Props}
import akka.io.IO
import akka.routing.SmallestMailboxPool

import spray.can.Http
import scala.slick.jdbc.JdbcBackend.DatabaseDef

import truerss.api.{RoutingService, WSApi}
import truerss.db.DbActor
import truerss.models.CurrentDriver
import truerss.config.TrueRSSConfig

class SystemActor(config: TrueRSSConfig,
                  dbDef: DatabaseDef,
                  driver: CurrentDriver) extends Actor {

  implicit val system = context.system

  val dbRef = context.actorOf(Props(new DbActor(dbDef, driver)), "db")

  val sourcesRef = context.actorOf(Props(new SourcesActor(
    config.appPlugins,
    self)), "sources")

  val proxyRef = context.actorOf(Props(
    new ProxyServiceActor(config.appPlugins, dbRef, sourcesRef))
      .withRouter(SmallestMailboxPool(10)), "service-router")

  val api = context.actorOf(Props(new RoutingService(proxyRef, config.wsPort)), "api")

  val socketApi = context.actorOf(Props(new WSApi(config.wsPort)), "ws-api")

  IO(Http) ! Http.Bind(api, interface = config.host, port = config.port)

  import global._

  def receive = {

    case StopApp => system.shutdown()

    case StopSystem => system.shutdown()

    case x => proxyRef forward x
  }

}
