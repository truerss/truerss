package truerss.db

import akka.actor.Actor
import akka.event.LoggingReceive
import akka.pattern._
import scala.concurrent.Future
import scala.slick.jdbc.JdbcBackend
import scala.slick.jdbc.JdbcBackend.DatabaseDef

import truerss.models.{CurrentDriver, Source, Feed}
import truerss.system
/**
 * Created by mike on 2.8.15.
 */
class DbActor(db: DatabaseDef, driver: CurrentDriver) extends Actor {

  import system.db._
  import driver.query._
  import driver.profile.simple._
  import context.dispatcher

  def receive = LoggingReceive {
    case GetAll => Future.successful{ db withSession { implicit session =>
      sources.buildColl
    }} pipeTo sender

    case GetSource(sourceId) => Future.successful {
      db withSession { implicit session =>
        sources.filter(_.id === sourceId).firstOption
      }
    } pipeTo sender

    case AddSource(source) => Future.successful {
      db withSession { implicit session =>
        (sources returning sources.map(_.id)) += source
      }
    } pipeTo sender


    case UrlIsUniq(url) =>
      Future.successful {
      db withSession { implicit session =>
        sources.filter(s => s.url === url).length.run
      }
    } pipeTo sender

    case NameIsUniq(name) => Future.successful {
      db withSession { implicit session =>
        sources.filter(s => s.name === name).length.run
      }
    } pipeTo sender

  }


}
