package truerss.system

import akka.actor.{ActorLogging, Actor, Props}
import truerss.db.DbActor
import truerss.models.CurrentDriver

import scala.slick.jdbc.JdbcBackend.DatabaseDef
import truerss.api.RoutingService
/**
 * Created by mike on 2.8.15.
 */
class SystemActor(dbDef: DatabaseDef, driver: CurrentDriver) extends Actor
 with ActorLogging {


  val dbRef = context.actorOf(Props(new DbActor(dbDef, driver)), "db")

  val proxyRef = context.actorOf(Props(new ProxyActor(dbRef)), "proxy")

  val api = context.actorOf(Props(new RoutingService(proxyRef)), "api")



  def receive = {
    case x => log.warning(s"Unexpected Message: ${x}")
  }

}
