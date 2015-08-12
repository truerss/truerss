package truerss.plugins

/**
 * Created by mike on 12.8.15.
 */
abstract class BasePlugin(config: Map[String, String] = Map.empty)
  extends BaseReader with PluginInfo
