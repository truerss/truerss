package truerss.plugins

/**
 * Created by mike on 12.8.15.
 */
trait PluginInfo { self : BasePlugin =>

  val author: String
  val about: String
  val pluginName: String
  val version: String
}


