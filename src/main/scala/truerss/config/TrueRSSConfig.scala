package truerss.config

import truerss.util.ApplicationPlugins

case class TrueRSSConfig(
  appDir: String = s"${System.getProperty("user.home")}/.truerss",
  host: String = "localhost",
  port: Int = 8000,
  wsPort: Int = 8080,
  appPlugins: ApplicationPlugins = ApplicationPlugins()
) {
  require(port != wsPort)
}
