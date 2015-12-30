package truerss
import java.util.Properties

import akka.actor.{ActorSystem, Props}

import com.typesafe.config.{ConfigException, ConfigFactory}
import com.zaxxer.hikari.{HikariDataSource, HikariConfig}

import java.io.File

import truerss.db._
import truerss.models.CurrentDriver
import truerss.system.SystemActor
import truerss.config.TrueRSSConfig
import truerss.util.PluginLoader

import scala.language.postfixOps
import scala.slick.jdbc.JdbcBackend
import scala.slick.jdbc.meta.MTable
import scala.util.control.Exception._
import scopt.OptionParser


object Boot extends App {
  import scala.collection.JavaConversions._

  val parser = new OptionParser[TrueRSSConfig]("truerss") {
    head("truerss", "0.0.1")
    opt[String]('d', "dir") action { (x, c) =>
      c.copy(appDir = x)
    } text "Base directory for truerss. By default it $HOME/.truerss"
    help("help") text "print usage text"
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

      if (pluginDirFile.exists()) {
        if (!pluginDirFile.canRead) {
          Console.err.println(s"""Add read access for ${pluginDir}""")
          sys.exit(1)
        }
      } else {
        pluginDirFile.mkdir()
      }

      val conf = ConfigFactory.parseFile(configFile)
      val appConfig = conf.getConfig("truerss")
      val dbConf = appConfig.getConfig("db")
      val pluginConf = appConfig.getConfig("plugins")

      val port = catching(classOf[ConfigException]) either
        appConfig.getInt("port") fold(_ => trueRSSConfig.port, identity)
      val host = catching(classOf[ConfigException]) either
        appConfig.getString("host") fold(_ => trueRSSConfig.host, identity)
      val wsPort = catching(classOf[ConfigException]) either
        appConfig.getInt("wsPort") fold(_ => trueRSSConfig.wsPort, identity)

      val appPlugins = PluginLoader.load(pluginDir, pluginConf)

      val need = Vector("backend", "port", "host", "dbname", "username", "password")
      val given = dbConf.entrySet().map(_.getKey).toVector

      val diff1 = given.diff(need)
      val diff2 = need.diff(given)
      if (diff1.nonEmpty) {
        Console.err.println(s"""Unexpected option name for 'db': ${diff1.mkString(",")}""")
        sys.exit(1)
      }
      if (diff2.nonEmpty) {
        Console.err.println(s"""Options '${diff2.mkString(",")}' for 'db' config not found""")
        sys.exit(1)
      }

      val dbBackend = dbConf.getString("backend")
      val dbHost = dbConf.getString("host")
      val dbPort = dbConf.getString("port")
      val dbName = dbConf.getString("dbname")
      val dbUsername = dbConf.getString("username")
      val dbPassword = dbConf.getString("password")

      val backend: Option[SupportedDb] = Some(H2)//DBProfile.get(dbBackend)//Some(H2)

      if (backend.isEmpty) {
        Console.err.println(s"Unsupported database backend: $dbBackend")
        sys.exit(1)
      }

      val dbProfile = DBProfile.create(backend.get)

      val db = backend.get match {
        case Sqlite =>
          val url =   s"jdbc:$dbBackend:/$dbName"
          JdbcBackend.Database.forURL(url, driver=dbProfile.driver)
        case H2 =>
          val url = "jdbc:h2:mem:test;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
          JdbcBackend.Database.forURL(url, driver = dbProfile.driver)
        case Postgresql | Mysql =>
          val props = new Properties()
          props.setProperty("dataSourceClassName", dbProfile.sourceClassName)
          props.setProperty("dataSource.user", dbUsername)
          props.setProperty("dataSource.password", dbPassword)
          props.setProperty("dataSource.databaseName", dbName)
          val hc = new HikariConfig(props)
          hc.setConnectionTestQuery("SELECT 1;")
          hc.setMaximumPoolSize(10)
          hc.setInitializationFailFast(true)
          try {
            val ds = new HikariDataSource(hc)
            JdbcBackend.Database.forDataSource(ds)
          } catch {
            case x: Exception =>
              Console.err.println("Database Initialization error. Check parameters for db.")
              sys.exit(1)
          }
      }

      val driver = new CurrentDriver(dbProfile.profile)

      import driver.profile.simple._

      import truerss.models.{Source, Neutral}
      import org.joda.time.DateTime

      db withSession { implicit session =>
        if (MTable.getTables("sources").list.isEmpty) {
          (driver.query.sources.ddl ++ driver.query.feeds.ddl).create
           val d = new DateTime().minusYears(1)
           val s = Source(id = None,
             url = "http://stackoverflow.com/feeds/tag?tagnames=java&sort=featured", //"https://news.ycombinator.com/rss", //
             name = "hacker news",
             interval = 12,
             state = Neutral,
             normalized = "hacker-news",
             lastUpdate = d.toDate
           )
//          val y = Source(id = None,
//            url = "https://www.youtube.com/feeds/videos.xml?channel_id=UC1kJkmSWt_snLDfuXgJnLnQ",
//            name = "youtube rethinkdb",
//            interval = 12,
//            state = Enable,
//            normalized = "youtube-rethinkdb",
//            lastUpdate = d.toDate
//          )
           driver.query.sources.insertAll(s)
        }
      }

      val actualConfig = trueRSSConfig.copy(
        port = port,
        host = host,
        wsPort = wsPort,
        appPlugins = appPlugins
      )

      implicit val system = ActorSystem("truerss")

      system.actorOf(Props(new SystemActor(actualConfig, db, driver)), "system-actor")

    case None =>
      Console.err.println("Unknown argument")
      sys.exit(1)
  }

}
