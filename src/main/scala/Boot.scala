package truerss

import akka.actor.{ActorSystem, Props}
import truerss.db.{DBProfile, H2}
import truerss.models.CurrentDriver
import truerss.system.SystemActor

import scala.language.postfixOps
import scala.slick.jdbc.JdbcBackend
import scala.slick.jdbc.meta.MTable


object Boot extends App {

  implicit val system = ActorSystem("truerss")

  val availableBackends = List("h2", "sqlite", "postgres")


  val dbProfile = DBProfile.create(H2)
  val db = JdbcBackend.Database.forURL("jdbc:h2:mem:test1;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = dbProfile.driver)
  val driver = new CurrentDriver(dbProfile.profile)

  import driver.profile.simple._


  db withSession { implicit session =>
    if (MTable.getTables("sources").list.isEmpty) {
      (driver.query.sources.ddl ++ driver.query.feeds.ddl).create
    }
  }

  system.actorOf(Props(new SystemActor(db, driver)), "system-actor")


}
