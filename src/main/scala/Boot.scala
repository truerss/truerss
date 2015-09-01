package truerss

import akka.actor.{ActorSystem, Props}

import com.typesafe.config.ConfigFactory

import java.io.File

import truerss.db.{DBProfile, H2}
import truerss.models.CurrentDriver
import truerss.system.SystemActor
import truerss.config.TrueRSSConfig
import truerss.util.PluginLoader

import scala.language.postfixOps
import scala.slick.jdbc.JdbcBackend
import scala.slick.jdbc.meta.MTable
import scopt.OptionParser

object Boot extends App {

  val parser = new OptionParser[TrueRSSConfig]("truerss") {
    head("truerss", "0.0.1")
    opt[String]('d', "dir") action { (x, c) =>
      c.copy(appDir = x)
    } text("Base directory for truerss. By default it $HOME/.truerss")
    help("help") text("print usage text")
  }

  parser.parse(args, TrueRSSConfig()) match {
    case Some(trueRSSConfig) =>
      val configFileName = "truerss.config"
      val appDir = trueRSSConfig.appDir
      val pluginDir = s"${appDir}/plugins"
      val configFile = new File(s"${appDir}/${configFileName}")
      if (!configFile.exists()) {
        Console.err.println(s"Config file ${configFileName} not exist in ${appDir}")
        sys.exit(1)
      }

      val pluginDirFile = new File(pluginDir)
      if (!pluginDirFile.canRead) {
        Console.err.println(s"""Add read access for ${pluginDir}""")
        sys.exit(1)
      }

      val conf = ConfigFactory.parseFile(configFile)
      val appConfig = conf.getConfig("truerss")
      val dbConf = appConfig.getConfig("db")
      val pluginConf = appConfig.getConfig("plugins")
      


    case None =>
      Console.err.println("Unknown argument")
      sys.exit(1)
  }

  implicit val system = ActorSystem("truerss")

  val availableBackends = List("h2", "sqlite", "postgres")


  val dbProfile = DBProfile.create(H2)
  val db = JdbcBackend.Database.forURL("jdbc:h2:mem:test1;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = dbProfile.driver)
  val driver = new CurrentDriver(dbProfile.profile)

  import driver.profile.simple._

  import truerss.models.Source
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
