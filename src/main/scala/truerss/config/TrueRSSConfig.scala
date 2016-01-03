package truerss.config

import truerss.util.ApplicationPlugins

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

object TrueRSSConfig {
  val db = "db"
  val plugins = "plugins"
  val root = "truerss"
  val updateParallelism = "update-parallelism"
}