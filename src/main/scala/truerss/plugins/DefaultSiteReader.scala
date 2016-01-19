package truerss.plugins

import java.net.URL

import com.github.truerss.ContentExtractor
import com.github.truerss.base.ContentTypeParam.{HtmlRequest, UrlRequest}
import com.github.truerss.base._
import com.typesafe.config.Config
import org.jsoup.Jsoup

import scala.collection.JavaConversions._
import scala.util.control.Exception._

import truerss.util.syntax.{\/, ext}

class DefaultSiteReader(config: Config)
  extends BaseSitePlugin(config) {
  import ext._
  import org.apache.logging.log4j.LogManager
  private final val logger = LogManager.getLogger("DefaultSiteReader")

  import Errors._
  import truerss.util.Request._

  implicit def exception2error(x: Throwable) = x match {
    case x: RuntimeException => ConnectionError(x.getMessage).left
    case x: Exception => UnexpectedError(x.getMessage).left
  }

  override val author = "fntz"
  override val about = "default rss|atom reader"
  override val pluginName = "Default"
  override val version = "0.0.3"
  override val contentType = Text
  override val contentTypeParam = ContentTypeParam.URL

  override val priority = -1

  override def matchUrl(url: URL) = true

  override def newEntries(url: String) = {
    catching(classOf[Exception]) either extract(url) fold(
      err => {
        logger.error(s"new entries error -> $url", err)
        err
      },
      either => either
    )
  }

  private def extract(url: String): Error \/ Vector[Entry] = {
    val response = getResponse(url)

    if (response.isError) {
      UnexpectedError(s"Connection error for $url with status code: ${response.code}").left
    } else {
      val x = scala.xml.XML.loadString(response.body)
      val parser = FeedParser.matchParser(x)
      val xs = parser.parse(x)

      // filter by empty url
      // transform url
      // filter description
      val result = xs.filter(_.url.isDefined)
        .map(p =>  p.copy(url = Some(normalizeLink(url, p.url.get))))
        .map { p =>
          p.description match {
            case Some(d) if d.contains("<img") =>
              p.copy(description =  Some(Jsoup.parse(d).select("img").remove().text()))
            case _ => p
          }
        }.map(_.toEntry).toVector

      result.right
    }
  }

  private def normalizeLink(url0: String, link: String): String = {
    val url = new URL(url0)
    val (protocol, host, port) = (url.getProtocol, url.getHost, url.getPort)

    val before = s"http"

    if (link.startsWith(before)) {
      link
    } else {
      val realPort = if (port == -1) {
        ""
      } else {
        s":$port"
      }
      s"$protocol://$host$realPort$link"
    }
  }

  override def content(urlOrContent: ContentTypeParam.RequestParam) = {
    urlOrContent match {
      case UrlRequest(url) =>
        catching(classOf[Exception]) either extractContent(url.toString) fold(
          err => {
            logger.error(s"content error -> $url", err.getMessage)
            UnexpectedError(err.getMessage).left
          },
          either => either
        )
      case HtmlRequest(_) => UnexpectedError("Pass url only").left
    }
  }

  private def extractContent(url: String): Error \/ Option[String] = {
    val response = getResponse(url)
    if (response.isError) {
      UnexpectedError(s"Connection error for $url").left
    } else {
      val url0 = new URL(url)
      val base = s"${url0.getProtocol}://${url0.getHost}"

      val doc = Jsoup.parse(response.body)
      val result = ContentExtractor.extract(doc.body())

      val need = doc.select(result.selector)

      need.select("img").foreach { img =>
        Option(img.absUrl("src")).map(img.attr("src", _)).getOrElse(img)
      }

      need.select("a").foreach { a =>
        val absUrl = a.attr("abs:href")
        if (absUrl.isEmpty) {
          a.attr("href", s"$base${a.attr("href")}")
        } else {
          a.attr("href", absUrl)
        }
      }

      need.select("form, input, meta, style, script").foreach(_.remove)

      need.html().some.right
    }
  }

}
