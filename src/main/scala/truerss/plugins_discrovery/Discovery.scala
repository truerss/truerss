package truerss.plugins_discrovery

import org.jsoup.Jsoup
import truerss.http_support.Request
import zio.{Task, ZIO}

import scala.jdk.CollectionConverters._

sealed trait Discovery {
  def url: String
  def isValidSource(url: String): Boolean
  def fetch(url: String): Task[Iterable[PluginJar]]
}

object Discovery {
  val available = Vector(GithubPluginDiscovery, LocalhostPluginDiscovery)

  def fetch(url: String): Task[Iterable[PluginJar]] = {
    available.find(_.isValidSource(url)).map { x =>
      x.fetch(url)
    }.getOrElse(ZIO.succeed(Nil))
  }
}

case object GithubPluginDiscovery extends Discovery {
  override val url: String = "https://github.com"

  override def isValidSource(url: String): Boolean = {
    url match {
      case s"https://github.com/$_/$_/releases/tag/$_" => true
      case _ => false
    }
  }
  // todo
  // unroll from github.com/org/repo => github.com/org/repo/releases/tag/latest
  //details a[rel='nofollow']
  override def fetch(url: String): Task[Iterable[PluginJar]] = {
    for {
      response <- Request.getResponseT(url)
    } yield fromHtml(response.body)
  }

  private def fromHtml(html: String): Iterable[PluginJar] = {
    val doc = Jsoup.parse(html)
    val elements = doc.body().select("details a")
    elements.asScala
      .filter(_.attr("rel") == "nofollow")
      .map(_.attr("href"))
      .filter(_.endsWith(".jar"))
      .map { u => s"$url/$u" }
      .map(PluginJar)
  }

}

// only for testing
case object LocalhostPluginDiscovery extends Discovery {
  override val url: String = "http://localhost"

  // localhost:9000/releases/123
  override def isValidSource(url: String): Boolean = {
    url match {
      case s"http://localhost:$_/releases/$_" => true
      case _ => false
    }
  }

  override def fetch(url: String): Task[Iterable[PluginJar]] = {
    ZIO.succeed(Nil)
  }
}