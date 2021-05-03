package truerss.services

import java.net.URL
import com.github.truerss.base._
import com.typesafe.config.Config
import truerss.dto.{ApplicationPlugins, PluginDto, PluginWithSourcePath, PluginsViewDto, SourceViewDto}
import truerss.db.{SourceState, SourceStates}
import truerss.plugins.PluginLoader
import zio.{Task, UIO}

import scala.util.Try

class ApplicationPluginsService(val pluginDir: String, val config: Config) {

  import ApplicationPluginsService._
  private type CR = BaseContentReader with UrlMatcher with Priority with PluginInfo

  @volatile protected var currentState: ApplicationPlugins = ApplicationPlugins()

  def reload(): Unit = {
    currentState = PluginLoader.load(pluginDir, config.getConfig(fPlugins))
  }

  def publishPlugins: Vector[BasePublishPlugin] =
    currentState.publishPlugins.map(_.plugin)

  def js: UIO[String] = Task.effectTotal(currentState.js.mkString)

  def css: UIO[String] = Task.effectTotal(currentState.css.mkString)

  def getState(url: String): SourceState = {
    currentState.getState(url)
  }

  def matchUrl(url: String): Boolean = {
    Try(matchUrl(new URL(url))).getOrElse(false)
  }

  def matchUrl(url: URL): Boolean = {
    currentState.inFeed(url) ||
      currentState.inContent(url) ||
      currentState.inSite(url)
  }

  def getFeedReader(url: URL): Option[BasePlugin] = {
    currentState.getFeedReader(url)
  }

  def getContentReader(url: URL): Option[BasePlugin] = {
    currentState.getContentReader(url)
  }

  def getContentReaderOrDefault(url: URL): BasePlugin = {
    currentState.getContentReaderOrDefault(url)
  }

  def view: UIO[PluginsViewDto] = {
    UIO.effectTotal {
      PluginsViewDto(
        feed = currentState.feedPlugins.map(baseToDto),
        content = currentState.contentPlugins.map(baseToDto),
        publish = currentState.publishPlugins.map(baseToDto),
        site = currentState.sitePlugins.map(baseToDto)
      )
    }
  }

  def getSourceReader(source: SourceViewDto): BaseFeedReader = {
    val url = new URL(source.url)
    source.state match {
      case SourceStates.Enable =>
        currentState.getSourceReader(url)
      case SourceStates.Neutral | SourceStates.Disable =>
        currentState.defaultPlugin
    }
  }

}

object ApplicationPluginsService {
  private val fPlugins = "plugins"
  private def baseToDto[T <: PluginInfo](x: PluginWithSourcePath[T]): PluginDto = {
    PluginDto(
      author = x.plugin.author,
      about = x.plugin.about,
      version = x.plugin.version,
      pluginName = x.plugin.pluginName,
      jarSourcePath = x.jarSourcePath
    )
  }

  implicit class ApplicationPluginsExt(val a: ApplicationPlugins) extends AnyVal {
    def getState(url: String): SourceState = {
      if (a.matchUrl(url)) {
        SourceStates.Enable
      } else {
        SourceStates.Neutral
      }
    }
  }
}