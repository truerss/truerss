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

  import truerss.models.Source
  import java.util.Date
  import org.joda.time.DateTime

  db withSession { implicit session =>
    if (MTable.getTables("sources").list.isEmpty) {
      (driver.query.sources.ddl ++ driver.query.feeds.ddl).create
      val d = new DateTime().minusYears(1)
      val s = Source(id = None,
        url = "https://news.ycombinator.com/rss",
        name = "hacker news",
        interval = 12,
        plugin = false,
        normalized = "hacker-news",
        lastUpdate = d.toDate,
        error = false
      )
      driver.query.sources.insert(s)
    }
  }

  system.actorOf(Props(new SystemActor(db, driver)), "system-actor")


}
