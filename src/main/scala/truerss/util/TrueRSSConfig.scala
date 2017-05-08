package truerss.util

import java.io.File

import com.typesafe.config.{Config, ConfigException, ConfigFactory}

import scopt.OptionParser

import scala.util.control.Exception._
import scala.collection.JavaConverters._

case class TrueRSSConfig(
  appDir: String = s"${System.getProperty("user.home")}/.truerss",
  host: String = "localhost",
  port: Int = 8000,
  wsPort: Int = 8080,
  parallelFeedUpdate: Int = 10, // update-parallelism
  appPlugins: ApplicationPlugins = ApplicationPlugins()
) {
  require(port != wsPort)
}

case class DbConfig(
                     dbBackend: String,
                     dbHost: String,
                     dbPort: String,
                     dbName: String,
                     dbUsername: String,
                     dbPassword: String
                   )
object DbConfig {
  val need = Vector("backend", "port", "host", "dbname", "username", "password")

  def load(dbConf: Config) = {
    val given = dbConf.entrySet().asScala.map(_.getKey).toVector

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

    DbConfig(
      dbBackend = dbBackend,
      dbHost = dbHost,
      dbPort = dbPort,
      dbName = dbName,
      dbUsername = dbUsername,
      dbPassword = dbPassword
    )
  }
}


object TrueRSSConfig {
  val db = "db"
  val plugins = "plugins"
  val root = "truerss"
  val updateParallelism = "update-parallelism"

  val parser = new OptionParser[TrueRSSConfig]("truerss") {
    head("truerss", "0.0.1")
    opt[String]('d', "dir") action { (x, c) =>
      c.copy(appDir = x)
    } text "Base directory for truerss. By default it $HOME/.truerss"
    help("help") text "print usage text"
  }

  def loadConfiguration(
                         trueRSSConfig: TrueRSSConfig
                       ): (TrueRSSConfig, DbConfig, Boolean) = {
    val configFileName = "truerss.config"
    val appDir = trueRSSConfig.appDir
    val confPath = s"$appDir/$configFileName"
    val pluginDir = s"$appDir/plugins"
    val userConfigFile = new File(confPath)

    val defaultConfigName = "default.conf"

    val (isUserConf, configFile) = if (!userConfigFile.exists()) {
      Console.println(s"Config file $configFileName not exist in $appDir")
      (false, new File(getClass.getClassLoader.getResource(defaultConfigName).getFile))
    } else {
      (true, userConfigFile)
    }

    val pluginDirFile = new File(pluginDir)

    if (pluginDirFile.exists()) {
      if (!pluginDirFile.canRead) {
        Console.err.println(s"""Add read access for $pluginDir""")
        sys.exit(1)
      }
    } else {
      pluginDirFile.mkdirs()
    }

    val conf = ConfigFactory.parseFile(configFile)
      .withFallback(ConfigFactory.parseString(
        scala.io.Source
          .fromInputStream(getClass.getClassLoader.getResourceAsStream(defaultConfigName)).mkString)
      )

    val appConfig = conf.getConfig(TrueRSSConfig.root)

    val pluginConf = appConfig.getConfig(TrueRSSConfig.plugins)
    val parallelFeed = catching(classOf[ConfigException]) either
      appConfig.getInt(TrueRSSConfig.updateParallelism) fold(e => 10, pf => pf)

    val port = catching(classOf[ConfigException]) either
      appConfig.getInt("port") fold(_ => trueRSSConfig.port, identity)
    val host = catching(classOf[ConfigException]) either
      appConfig.getString("host") fold(_ => trueRSSConfig.host, identity)
    val wsPort = catching(classOf[ConfigException]) either
      appConfig.getInt("wsPort") fold(_ => trueRSSConfig.wsPort, identity)

    val appPlugins = PluginLoader.load(pluginDir, pluginConf)

    val dbConf = appConfig.getConfig(TrueRSSConfig.db)
    val dbConfig = DbConfig.load(dbConf)

    (trueRSSConfig.copy(
      port = port,
      host = host,
      wsPort = wsPort,
      parallelFeedUpdate = parallelFeed,
      appPlugins = appPlugins
    ), dbConfig, isUserConf)
  }


}