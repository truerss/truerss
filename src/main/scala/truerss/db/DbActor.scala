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

    case DeleteSource(sourceId) => Future.successful {
      db withSession { implicit session =>
        val res = sources.filter(_.id === sourceId).firstOption
        sources.filter(_.id === sourceId).delete
        res
      }
    } pipeTo sender

    case AddSource(source) => Future.successful {
      db withSession { implicit session =>
        (sources returning sources.map(_.id)) += source
      }
    } pipeTo sender

    case UpdateSource(num, source) => Future.successful {
      db withSession { implicit session =>
        sources.filter(_.id === source.id)
          .map(s => (s.url, s.name, s.interval, s.plugin, s.normalized))
          .update(source.url, source.name, source.interval,
            source.plugin, source.normalized)
      }
    } pipeTo sender


    case UrlIsUniq(url, id) =>
      Future.successful {
      db withSession { implicit session =>
        id.map(id => sources.filter(s => s.url === url && s.id != id))
          .getOrElse(sources.filter(s => s.url === url)).length.run
      }
    } pipeTo sender

    case NameIsUniq(name, id) => Future.successful {
      db withSession { implicit session =>
        id.map(id => sources.filter(s => s.name === name && s.id != id))
        .getOrElse(sources.filter(s => s.name === name)).length.run
      }
    } pipeTo sender

  }


}
