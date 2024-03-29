package truerss.plugins

import com.github.truerss.base._
import com.typesafe.config.ConfigFactory

import java.net.URI
import scala.util.Try

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

  def getFeedReader(url: URI): Option[BasePlugin] = {
    (feedsP.filter(_.matchUrl(url)) ++
      sitesP.filter(_.matchUrl(url)))
      .sortBy(_.priority).reverse.headOption
  }

  def getContentReader(url: URI): Option[BasePlugin] = {
    (contentsP.filter(_.matchUrl(url)) ++
      sitesP.filter(_.matchUrl(url)))
      .sortBy(_.priority).reverse.headOption
  }

  def getContentReaderOrDefault(url: URI): BasePlugin = {
    getContentReader(url).getOrElse(defaultPlugin)
  }

  def getSourceReader(url: URI): BaseFeedReader = {
    (getFeedReader(url), getContentReader(url)) match {
      case (None, None) =>
        defaultPlugin
      case (f, _) =>
        f.getOrElse(defaultPlugin).asInstanceOf[BaseFeedReader]
      case _ =>
        defaultPlugin
    }
  }


  def inFeed(url: URI): Boolean = {
    feedsP.exists(_.matchUrl(url))
  }

  def inContent(url: URI): Boolean = {
    contentsP.exists(_.matchUrl(url))
  }

  def inSite(url: URI): Boolean = {
    sitesP.exists(_.matchUrl(url))
  }

  def matchUrl(url: URI): Boolean = {
    inFeed(url) || inContent(url) || inSite(url)
  }

  def matchUrl(url: String): Boolean = {
    Try(matchUrl(URI.create(url))).getOrElse(false)
  }

}
