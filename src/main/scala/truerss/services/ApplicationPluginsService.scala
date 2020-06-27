package truerss.services

import java.net.URL

import com.github.truerss.base._
import com.typesafe.config.ConfigFactory
import truerss.dto.{PluginDto, PluginsViewDto, SourceViewDto}
import truerss.db.SourceStates
import truerss.plugins.DefaultSiteReader
import truerss.util.ApplicationPlugins

class ApplicationPluginsService(appPlugins: ApplicationPlugins) {

  import ApplicationPluginsService._
  private type CR = BaseContentReader with UrlMatcher with Priority with PluginInfo

  def js: String = appPlugins.js.mkString

  def css: String = appPlugins.css.mkString

  def matchUrl(url: URL): Boolean = {
    appPlugins.inFeed(url) ||
      appPlugins.inContent(url) ||
      appPlugins.inSite(url)
  }

  def getFeedReader(url: URL): Option[BasePlugin] = {
    appPlugins.getFeedReader(url)
  }

  def getContentReader(url: URL): Option[BasePlugin] = {
    appPlugins.getContentReader(url)
  }

  def getContentReaderOrDefault(url: URL): BasePlugin = {
    appPlugins.getContentReaderOrDefault(url)
  }

  def view: PluginsViewDto = {
    PluginsViewDto(
      feed = appPlugins.feedPlugins.map(baseToDto),
      content = appPlugins.contentPlugins.map(baseToDto),
      publish = appPlugins.publishPlugins.map(baseToDto),
      site = appPlugins.sitePlugins.map(baseToDto)
    )
  }

  def getSourceReader(source: SourceViewDto): BaseFeedReader = {
    val url = new URL(source.url)
    source.state match {
      case SourceStates.Neutral =>
        appPlugins.defaultPlugin
      case SourceStates.Enable =>
        val feedReader = getFeedReader(url)
        val contentReader = getContentReader(url)

        (feedReader, contentReader) match {
          case (None, None) =>
            appPlugins.defaultPlugin
          case (f, _) =>
            val f0 = f.getOrElse(appPlugins.defaultPlugin)
            f0.asInstanceOf[BaseFeedReader]
          case _ =>
            appPlugins.defaultPlugin
        }

      case SourceStates.Disable =>
        appPlugins.defaultPlugin
    }
  }

}

object ApplicationPluginsService {
  private def baseToDto[T <: PluginInfo](x: T): PluginDto = {
    PluginDto(
      author = x.author,
      about = x.about,
      version = x.version,
      pluginName = x.pluginName
    )
  }
}