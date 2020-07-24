package truerss.util

import java.io.File

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import scopt.OptionParser
import truerss.dto.ApplicationPlugins
import truerss.plugins.YoutubePlugin

import scala.util.control.Exception._
import scala.collection.JavaConverters._

case class TrueRSSConfig(
  appDir: String = s"${System.getProperty("user.home")}/.truerss",
  host: String = "localhost",
  port: Int = 8000,
  wsPort: Int = 8080,
  feedParallelism: Int = 10, // update-parallelism
  appPlugins: ApplicationPlugins = ApplicationPlugins()
) {
  require(port != wsPort)

  val url = s"$host:$port"

  def withParallelism(count: Int): TrueRSSConfig = {
    copy(feedParallelism = count)
  }
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

  private object Fields {
    val fBackend = "backend"
    val fPort = "port"
    val fHost = "host"
    val fDbName = "dbname"
    val fUserName = "username"
    val fPassword = "password"
  }
  import Fields._

  val need = Vector(fBackend, fPort, fHost, fDbName, fUserName, fPassword)

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

    val dbBackend = dbConf.getString(fBackend)
    val dbHost = dbConf.getString(fHost)
    val dbPort = dbConf.getString(fPort)
    val dbName = dbConf.getString(fDbName)
    val dbUsername = dbConf.getString(fUserName)
    val dbPassword = dbConf.getString(fPassword)

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
  private object Fields {
    val fDbb = "db"
    val fPlugins = "plugins"
    val fRoot = "truerss"
    val fUpdateParallelism = "update-parallelism"

    val fPort = "port"
    val fHost = "host"
    val fWsPort = "wsPort"
  }

  import Fields._

  val parser = new OptionParser[TrueRSSConfig]("truerss") {
    head("truerss", "0.0.1")
    opt[String]('d', "dir") action { (x, c) =>
      c.copy(appDir = x)
    } text "Base directory for truerss. By default it $HOME/.truerss"
    help("help") text "print usage text"
  }

  def loadConfiguration(trueRSSConfig: TrueRSSConfig): (TrueRSSConfig, DbConfig, Boolean) = {
    val configFileName = "truerss.config"
    val appDir = trueRSSConfig.appDir
    val confPath = s"$appDir/$configFileName"
    val pluginDir = s"$appDir/plugins"
    val userConfigFile = new File(confPath)

    val defaultConfigName = "default.conf"

    val (isUserConf, configFile) = if (!userConfigFile.exists()) {
      Console.println(s"Config file '$configFileName' was not found in '$appDir'")
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

    val appConfig = conf.getConfig(fRoot)

    val pluginConf = appConfig.getConfig(fPlugins)

    val port = catching(classOf[ConfigException]) either
      appConfig.getInt(fPort) fold(_ => trueRSSConfig.port, identity)
    val host = catching(classOf[ConfigException]) either
      appConfig.getString(fHost) fold(_ => trueRSSConfig.host, identity)
    val wsPort = catching(classOf[ConfigException]) either
      appConfig.getInt(fWsPort) fold(_ => trueRSSConfig.wsPort, identity)

    val appPlugins = PluginLoader.load(pluginDir, pluginConf)
      .addPlugin(new YoutubePlugin(pluginConf))

    val dbConf = appConfig.getConfig(fDbb)
    val dbConfig = DbConfig.load(dbConf)

    (trueRSSConfig.copy(
      port = port,
      host = host,
      wsPort = wsPort,
      appPlugins = appPlugins
    ), dbConfig, isUserConf)
  }


}