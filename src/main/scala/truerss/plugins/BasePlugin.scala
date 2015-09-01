package truerss.plugins

/**
 * Created by mike on 12.8.15.
 */
abstract class BasePlugin(override val config: Map[String, String] = Map.empty)
  extends ConfigProvider

abstract class BaseFeedPlugin(config: Map[String, String] = Map.empty)
  extends BasePlugin(config)
  with ConfigProvider
  with BaseFeedReader
  with PluginInfo
  with Priority
  with UrlMatcher

abstract class BaseContentPlugin(config: Map[String, String] = Map.empty)
  extends BasePlugin(config)
  with ConfigProvider
  with BaseContentReader
  with PluginInfo
  with Priority
  with UrlMatcher

abstract class BaseSitePlugin(config: Map[String, String] = Map.empty)
  extends BasePlugin(config)
  with BaseContentReader
  with BaseFeedReader
  with ConfigProvider
  with PluginInfo
  with Priority
  with UrlMatcher

abstract class BasePublishPlugin(config: Map[String, String] = Map.empty)
  extends BasePlugin(config)
  with PublishPlugin
  with ConfigProvider
  with PluginInfo

