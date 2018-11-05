package truerss.services

import java.net.URL

import com.github.truerss.base._
import com.typesafe.config.ConfigFactory
import truerss.dto.{PluginDto, PluginsViewDto, SourceViewDto}
import truerss.db.SourceStates
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

  def getContentReaderOrDefault(url: URL): BasePlugin = {
    getContentReader(url).getOrElse(defaultPlugin)
  }

  def view: PluginsViewDto = {
    PluginsViewDto(
      feed = appPlugins.feedPlugins.map(baseToDto).toVector,
      content = appPlugins.contentPlugins.map(baseToDto).toVector,
      publish = appPlugins.publishPlugins.map(baseToDto).toVector,
      site = appPlugins.sitePlugins.map(baseToDto).toVector
    )
  }

  def getSourceReader(source: SourceViewDto): BaseFeedReader = {
    val url = new URL(source.url)
    source.state match {
      case SourceStates.Neutral =>
        defaultPlugin
      case SourceStates.Enable =>
        val feedReader = getFeedReader(url)
        val contentReader = getContentReader(url)

        (feedReader, contentReader) match {
          case (None, None) =>
            defaultPlugin
          case (f, _) =>
            val f0 = f.getOrElse(defaultPlugin)
            f0.asInstanceOf[BaseFeedReader]
          case _ =>
            defaultPlugin
        }

      case SourceStates.Disable =>
        defaultPlugin
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
