package truerss.services

import java.net.URL

import com.github.truerss.base._
import com.typesafe.config.ConfigFactory
import truerss.dto.{PluginDto, PluginsViewDto, SourceViewDto}
import truerss.models.{Disable, Enable, Neutral}
import truerss.plugins.DefaultSiteReader
import truerss.util.ApplicationPlugins

class ApplicationPluginsService(appPlugins: ApplicationPlugins) {

  private type CR = BaseContentReader with UrlMatcher with Priority with PluginInfo

  // move to app plugins TODO
  protected val defaultPlugin = new DefaultSiteReader(ConfigFactory.empty())

  val plugins = appPlugins.contentPlugins.toVector

  val contentReaders: Vector[CR] = appPlugins.contentPlugins.toVector ++ Vector(defaultPlugin)

  def js: String = appPlugins.js.mkString

  def css: String = appPlugins.css.mkString

  def matchUrl(url: URL): Boolean = {
    appPlugins.feedPlugins.exists(_.matchUrl(url)) ||
      appPlugins.contentPlugins.exists(_.matchUrl(url)) ||
      appPlugins.sitePlugins.exists(_.matchUrl(url))
  }

  def getFeedReader(url: URL): Option[BasePlugin] = {
    (appPlugins.feedPlugins.filter(_.matchUrl(url)) ++
      appPlugins.sitePlugins.filter(_.matchUrl(url)))
      .sortBy(_.priority).reverse.headOption
  }

  def getContentReader(url: URL): Option[BasePlugin] = {
    (appPlugins.contentPlugins.filter(_.matchUrl(url)) ++
      appPlugins.sitePlugins.filter(_.matchUrl(url)))
      .sortBy(_.priority).reverse.headOption
  }

  def view: PluginsViewDto = {
    PluginsViewDto(
      feed = appPlugins.feedPlugins.map(baseToDto).toVector,
      content = appPlugins.contentPlugins.map(baseToDto).toVector,
      publish = appPlugins.publishPlugins.map(baseToDto).toVector,
      site = appPlugins.sitePlugins.map(baseToDto).toVector
    )
  }

  def getSourceReader(source: SourceViewDto) = {
    val url = new URL(source.url)
    source.state match {
      case Neutral =>
        Some(defaultPlugin)
      case Enable =>
        val feedReader = getFeedReader(url)

        val contentReader = getContentReader(url)

        (feedReader, contentReader) match {
          case (None, None) =>
//            logger.warn(s"Disable ${source.id} -> ${source.name} Source. " +
//              s"Plugin not found")

            //stream.publish(DbHelperActor.SetState(source.id, Disable))

            None
          case (f, c) =>
            val f0 = f.getOrElse(defaultPlugin)
            val c0 = c.getOrElse(defaultPlugin)
//            logger.info(s"${source.name} need plugin." +
//              s" Detect feed plugin: ${f0.pluginName}, " +
//              s" content plugin: ${c0.pluginName}")
            Some(f0)
        }

      case Disable => None

    }
  }

  private def baseToDto[T <: PluginInfo](x: T): PluginDto = {
    PluginDto(
      author = x.author,
      about = x.about,
      version = x.version,
      pluginName = x.pluginName
    )
  }

}
