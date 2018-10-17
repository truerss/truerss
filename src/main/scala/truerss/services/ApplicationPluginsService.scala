package truerss.services

import java.net.URL

import com.github.truerss.base.{BasePlugin, PluginInfo}
import truerss.dto.{PluginDto, PluginsViewDto}
import truerss.util.ApplicationPlugins

class ApplicationPluginsService(appPlugins: ApplicationPlugins) {

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

  private def baseToDto[T <: PluginInfo](x: T): PluginDto = {
    PluginDto(
      author = x.author,
      about = x.about,
      version = x.version,
      pluginName = x.pluginName
    )
  }

}
