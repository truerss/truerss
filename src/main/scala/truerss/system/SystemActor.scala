package truerss.system

import akka.actor.{Actor, Props}
import akka.io.IO
import akka.routing.SmallestMailboxPool

import spray.can.Http
import scala.slick.jdbc.JdbcBackend.DatabaseDef

import truerss.api.{RoutingService, WSApi}
import truerss.db.DbActor
import truerss.models.CurrentDriver

/**
 * Created by mike on 2.8.15.
 */
class SystemActor(dbDef: DatabaseDef, driver: CurrentDriver) extends Actor {

  implicit val system = context.system

  val dbRef = context.actorOf(Props(new DbActor(dbDef, driver)), "db")

  val networkRef = context.actorOf(
    Props(new NetworkActor).withRouter(SmallestMailboxPool(10)), "network-router")

  val sourcesRef = context.actorOf(Props(new SourcesActor(self, networkRef)), "sources")

  val proxyRef = context.actorOf(Props(
    new ProxyServiceActor(dbRef, networkRef, sourcesRef))
      .withRouter(SmallestMailboxPool(10)), "service-router")

  val api = context.actorOf(Props(new RoutingService(proxyRef)), "api")

  val socketApi = context.actorOf(Props(new WSApi(8080)), "ws-api")

  IO(Http) ! Http.Bind(api, interface = "localhost", port = 8000)


  def receive = {
    case x => proxyRef forward x
  }

}
