package truerss.dto

import java.net.URL

import com.github.truerss.base._
import com.typesafe.config.ConfigFactory
import truerss.plugins.DefaultSiteReader

import scala.util.Try

case class PluginWithSourcePath[T <: PluginInfo](
                     plugin: T,
                     jarSourcePath: String
                   )

case class ApplicationPlugins(
                               feedPlugins: Vector[PluginWithSourcePath[BaseFeedPlugin]] = Vector.empty,
                               contentPlugins: Vector[PluginWithSourcePath[BaseContentPlugin]] = Vector.empty,
                               publishPlugins: Vector[PluginWithSourcePath[BasePublishPlugin]] = Vector.empty,
                               sitePlugins: Vector[PluginWithSourcePath[BaseSitePlugin]] = Vector.empty,
                               css: Vector[String] = Vector.empty, // content of js files
                               js: Vector[String] = Vector.empty
                             ) {

  final val defaultPlugin = new DefaultSiteReader(ConfigFactory.empty())

  private val feedsP = feedPlugins.map(_.plugin)
  private val sitesP = sitePlugins.map(_.plugin)
  private val contentsP = contentPlugins.map(_.plugin)

  def getFeedReader(url: URL): Option[BasePlugin] = {
    (feedsP.filter(_.matchUrl(url)) ++
      sitesP.filter(_.matchUrl(url)))
      .sortBy(_.priority).reverse.headOption
  }

  def getContentReader(url: URL): Option[BasePlugin] = {
    (contentsP.filter(_.matchUrl(url)) ++
      sitesP.filter(_.matchUrl(url)))
      .sortBy(_.priority).reverse.headOption
  }

  def getContentReaderOrDefault(url: URL): BasePlugin = {
    getContentReader(url).getOrElse(defaultPlugin)
  }

  def getSourceReader(url: URL): BaseFeedReader = {
    (getFeedReader(url), getContentReader(url)) match {
      case (None, None) =>
        defaultPlugin
      case (f, _) =>
        f.getOrElse(defaultPlugin).asInstanceOf[BaseFeedReader]
      case _ =>
        defaultPlugin
    }
  }


  def inFeed(url: URL): Boolean = {
    feedsP.exists(_.matchUrl(url))
  }

  def inContent(url: URL): Boolean = {
    contentsP.exists(_.matchUrl(url))
  }

  def inSite(url: URL): Boolean = {
    sitesP.exists(_.matchUrl(url))
  }

  def matchUrl(url: URL): Boolean = {
    feedsP.exists(_.matchUrl(url)) ||
      contentsP.exists(_.matchUrl(url)) ||
      sitesP.exists(_.matchUrl(url))
  }

  def matchUrl(url: String): Boolean = {
    Try(matchUrl(new java.net.URL(url))).toOption.getOrElse(false)
  }

}
