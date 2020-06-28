package truerss.dto

import java.net.URL

import com.github.truerss.base._
import com.typesafe.config.ConfigFactory
import truerss.plugins.DefaultSiteReader

import scala.util.Try

case class ApplicationPlugins(
                               feedPlugins: Vector[BaseFeedPlugin] = Vector.empty,
                               contentPlugins: Vector[BaseContentPlugin] = Vector.empty,
                               publishPlugins: Vector[BasePublishPlugin] = Vector.empty,
                               sitePlugins: Vector[BaseSitePlugin] = Vector.empty,
                               css: Vector[String] = Vector.empty, // content of js files
                               js: Vector[String] = Vector.empty
                             ) {

  final val defaultPlugin = new DefaultSiteReader(ConfigFactory.empty())

  def getFeedReader(url: URL): Option[BasePlugin] = {
    (feedPlugins.filter(_.matchUrl(url)) ++
      sitePlugins.filter(_.matchUrl(url)))
      .sortBy(_.priority).reverse.headOption
  }

  def getContentReader(url: URL): Option[BasePlugin] = {
    (contentPlugins.filter(_.matchUrl(url)) ++
      sitePlugins.filter(_.matchUrl(url)))
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
    feedPlugins.exists(_.matchUrl(url))
  }

  def inContent(url: URL): Boolean = {
    contentPlugins.exists(_.matchUrl(url))
  }

  def inSite(url: URL): Boolean = {
    sitePlugins.exists(_.matchUrl(url))
  }

  def matchUrl(url: URL): Boolean = {
    feedPlugins.exists(_.matchUrl(url)) ||
      contentPlugins.exists(_.matchUrl(url)) ||
      sitePlugins.exists(_.matchUrl(url))
  }

  def matchUrl(url: String): Boolean = {
    Try(matchUrl(new java.net.URL(url))).toOption.getOrElse(false)
  }

  def addPlugin(plugin: BasePlugin): ApplicationPlugins = {
    plugin match {
      case x: BaseFeedPlugin =>
        copy(feedPlugins = feedPlugins :+ x)
      case x: BaseContentPlugin =>
        copy(contentPlugins = contentPlugins :+ x)
      case x: BasePublishPlugin =>
        copy(publishPlugins = publishPlugins :+ x)
      case x: BaseSitePlugin =>
        copy(sitePlugins = sitePlugins :+ x)
    }
  }

}
