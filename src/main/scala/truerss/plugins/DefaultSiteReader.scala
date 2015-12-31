package truerss.plugins

import java.io.ByteArrayInputStream
import java.net.URL
import java.util.Date

import com.github.truerss.ContentExtractor
import com.github.truerss.base.ContentTypeParam.{HtmlRequest, UrlRequest}
import com.github.truerss.base._
import com.rometools.rome.io.{ParsingFeedException => PE, SyndFeedInput, XmlReader}
import com.typesafe.config.Config
import org.jsoup.Jsoup

import scala.collection.JavaConversions._
import scala.util.control.Exception._

class DefaultSiteReader(config: Config)
  extends BaseSitePlugin(config) {

  import org.apache.logging.log4j.LogManager
  private final val logger = LogManager.getLogger("DefaultSiteReader")

  import Errors._
  import truerss.util.Request._

  implicit def exception2error(x: Throwable) = x match {
    case x: PE => Left(ParsingError(x.getMessage))
    case x: RuntimeException => Left(ConnectionError(x.getMessage))
    case x: Exception => Left(UnexpectedError(x.getMessage))
  }

  override val author = "fntz"
  override val about = "default rss|atom reader"
  override val pluginName = "Default"
  override val version = "0.0.3"
  override val contentType = Text
  override val contentTypeParam = ContentTypeParam.URL

  override val priority = -1

  val sfi = new SyndFeedInput()

  override def matchUrl(url: URL) = true

  override def newEntries(url: String) = {
    catching(classOf[Exception]) either extract(url) fold(
      err => {
        logger.error(s"new entries error -> ${url}", err)
        err
      },
      ok => Right(ok)
      )
  }

  private def extract(url: String) = {
    val response = getResponse(url)

    if (response.isError) {
      throw new RuntimeException(s"Connection error for ${url} with status code: ${response.code}")
    }
    val asBytes = response.body
      .trim
      .replaceAll("[^\\x20-\\x7e\\x0A]", "")
      .getBytes("UTF-8")

    val xml = new XmlReader(new ByteArrayInputStream(asBytes))
    val feed = sfi.build(xml)

    val entries = feed.getEntries.zipWithIndex.collect {
      case p @ (entry, index) =>
        val author = entry.getAuthor
        val date = Option(entry.getPublishedDate).getOrElse(new Date())
        val title = Option(entry.getTitle).map(_.trim)


        val link = (Option(entry.getLink) ++ Option(entry.getUri))
          .reduceLeftOption { (link, uri) =>
            if (link != "") {
              link
            } else {
              uri
            }
          }.getOrElse {
            throw new RuntimeException(s"Impossible extract feeds for $url")
          }

        val cont = None

        val description = Option(entry.getDescription)
          .map(d => Jsoup.parse(d.getValue).select("img").remove().text()).
          orElse(None)

        val d = if (description.map(_.trim.length).getOrElse(0) == 0) {
          None
        } else {
          description
        }

        Entry(normalizeLink(url, link), title.getOrElse(s"No title-$index"),
          author, date, d, cont)
    }
    entries.toVector.reverse
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
            Left(UnexpectedError(err.getMessage))
          },
          ok => Right(ok)
        )
      case HtmlRequest(_) => Left(UnexpectedError("Pass url only"))
    }
  }

  private def extractContent(url: String) = {
    val response = getResponse(url)

    if (response.isError) {
      throw new RuntimeException(s"Connection error for ${url}")
    }

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

    Some(need.html())
  }

}
