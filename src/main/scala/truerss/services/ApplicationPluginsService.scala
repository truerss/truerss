package truerss.services

import java.net.URL

import com.github.truerss.base._
import truerss.dto.{ApplicationPlugins, PluginDto, PluginsViewDto, SourceViewDto}
import truerss.db.SourceStates
import zio.{Task, UIO}

class ApplicationPluginsService(appPlugins: ApplicationPlugins) {

  import ApplicationPluginsService._
  private type CR = BaseContentReader with UrlMatcher with Priority with PluginInfo

  def js: UIO[String] = Task.effectTotal(appPlugins.js.mkString)

  def css: UIO[String] = Task.effectTotal(appPlugins.css.mkString)

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

  def view: UIO[PluginsViewDto] = {
    UIO.effectTotal {
      PluginsViewDto(
        feed = appPlugins.feedPlugins.map(baseToDto),
        content = appPlugins.contentPlugins.map(baseToDto),
        publish = appPlugins.publishPlugins.map(baseToDto),
        site = appPlugins.sitePlugins.map(baseToDto)
      )
    }
  }

  def getSourceReader(source: SourceViewDto): BaseFeedReader = {
    val url = new URL(source.url)
    source.state match {
      case SourceStates.Enable =>
        appPlugins.getSourceReader(url)
      case SourceStates.Neutral | SourceStates.Disable =>
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