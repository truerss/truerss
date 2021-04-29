package truerss.plugins

import java.net.URL

import com.github.truerss.ContentExtractor
import com.github.truerss.base.ContentTypeParam.{HtmlRequest, UrlRequest}
import com.github.truerss.base._
import com.typesafe.config.Config
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.slf4j.LoggerFactory
import scalaj.http.HttpResponse
import truerss.http_support.Request

import scala.jdk.CollectionConverters._
import truerss.util.syntax.{\/, ext}
import truerss.util.{EntryImplicits, CommonImplicits}

import scala.util.Try

class DefaultSiteReader(config: Config)
  extends BaseSitePlugin(config) with Request {

  import DefaultSiteReader._
  import Errors._
  import ext._

  private val logger = LoggerFactory.getLogger(getClass)

  implicit def exception2error(x: Throwable): Either[Error, Nothing] = x match {
    case x: RuntimeException => ConnectionError(x.getMessage).left
    case x: Exception => UnexpectedError(x.getMessage).left
  }

  override val author = "fntz"
  override val about = "default rss|atom reader"
  override val pluginName = "Default"
  override val version = "0.0.3"
  override val contentType: BaseType = Text
  override val contentTypeParam: ContentTypeParam.URL.type = ContentTypeParam.URL

  override val priority: Int = -1

  override def matchUrl(url: URL) = true

  override def newEntries(url: String): Error \/ Vector[Entry] = {
    val response = getResponse(url)

    if (response.isError) {
      UnexpectedError(s"Connection error for $url with status code: ${response.code}").left
    } else {
      Try(extractEntries(url, response.body)).fold(
        error => {
          logger.warn(s"Failed to fetch entries from $url", error)
          UnexpectedError(s"Failed to fetch entries from $url").left
        },
        _.toVector.right
      )
    }
  }

  override def content(urlOrContent: ContentTypeParam.RequestParam): Error \/ Option[String] = {
    urlOrContent match {
      case UrlRequest(tmp) =>
        val url = tmp.toString
        val response = getResponse(url)
        if (response.isError) {
          UnexpectedError(s"Connection error for $url").left
        } else {
          val result = parseContent(url, response.body)

          Some(result).right
        }
      case HtmlRequest(_) => UnexpectedError("Pass url only").left
    }
  }

  override def getResponse(url: String): HttpResponse[String] = {
    super.getResponse(url)
  }

}

object DefaultSiteReader {

  import CommonImplicits._
  import EntryImplicits._

  val forbidElements = Iterable("form", "input", "meta", "style", "script")
  private val href = "href"

  def extractEntries(url: String, body: String): Iterable[Entry] = {
    val currentXml = scala.xml.XML.loadString(body)
    val parser = FeedParser.matchParser(currentXml)
    val xs = parser.parse(currentXml)

    // filter by empty url
    // transform url
    // filter description
    xs.filter(_.url.isDefined)
      .map { p => p.copy(url = Some(normalizeLink(url, p.url.get))) }
      .map { p => p.clearImages }
      .map(_.toEntry)
  }

  // float link to hard
  def normalizeLink(url0: String, link: String): String = {
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

  def parseContent(url: String, body: String): String = {
    val base = url.toUrl.toBase

    val doc = Jsoup.parse(body)
    val result = ContentExtractor.extract(doc.body())

    val need = doc.select(result.selector)

    need.select("img").asScala.foreach { img =>
      changeUrl(img, "src", "src", base)
    }

    need.select("a").asScala.foreach { a =>
      changeUrl(a, href, "abs:href", base)
    }

    forbidElements.foreach { element =>
      need.select(element).asScala.foreach(_.remove())
    }
    need.html()
  }

  private def changeUrl(element: Element, key: String, keyAttr: String, base: String): Element = {
    val absUrl = element.attr(keyAttr)
    if (absUrl.isEmpty || !absUrl.startsWith("http")) {
      element.attr(key, s"$base${element.attr(key)}")
    } else {
      element
    }
  }

}
