package truerss.config

case class TrueRSSConfig(
  appDir: String = System.getProperty("user.home"),
  host: String = "localhost",
  port: Int = 8000,
  wsPort: Int = 8080,
  pluginSetting: Map[String, Map[String, String]] = Map.empty
) {
  require(port != wsPort)
}
