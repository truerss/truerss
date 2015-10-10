package truerss.plugins

import java.io.ByteArrayInputStream
import java.util.Date
import java.net.URL

import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.io.{SyndFeedInput, XmlReader}
import org.jsoup.Jsoup
import org.jsoup.parser._

import scala.collection.JavaConversions._
import scala.util.control.Exception._
import com.rometools.rome.io.{ParsingFeedException => PE}

import com.github.truerss.ContentExtractor
import com.github.truerss.base.{Errors, BaseSitePlugin, Entry, Text}

class DefaultSiteReader(config: Map[String, String])
  extends BaseSitePlugin(config) {

  import truerss.util.Request._
  import Errors._


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

  override val priority = 0

  val sfi = new SyndFeedInput()

  override def matchUrl(url: String) = true

  override def newEntries(url: String) = {
    catching(classOf[Exception]) either extract(url) fold(
      err => err,
      ok => Right(ok)
      )
  }

  private def extract(url: String) = {
    val response = getResponse(url)

    if (response.isError) {
      throw new RuntimeException(s"Connection error for ${url}")
    }

    val source = Jsoup.connect(url).ignoreContentType(true).parser(Parser.xmlParser()).get()
    val result = source.html()

    val x = new XmlReader(new ByteArrayInputStream(result.getBytes()))
    val feed = sfi.build(x)

    val entries = feed.getEntries.collect {
      case entry: SyndEntry =>
        val author = entry.getAuthor
        val date = Option(entry.getPublishedDate).getOrElse(new Date())
        val title = Option(entry.getTitle).map(_.trim)

        val link = if (entry.getLink != "") {
          entry.getLink.trim
        } else {
          entry.getUri.trim
        }

        val cont = None

        val description = Option(entry.getDescription)
          .map(d => Jsoup.parse(d.getValue).select("img").remove().text()).
          orElse(None)

        val d = if (description.map(_.trim.size).getOrElse(0) == 0) {
          None
        } else {
          description
        }

        Entry(normalizeLink(url, link), title.get, author, date, d, cont)
    }

    entries.toVector.reverse
  }

  private def normalizeLink(url0: String, link: String): String = {
    val url = new URL(url0)
    val (protocol, host, port) = (url.getProtocol, url.getHost, url.getPort)

    val before = s"$protocol://$host"

    if (link.startsWith(before)) {
      link
    } else {
      val realPort = if (port == -1) {
        ""
      } else {
        s":$port"
      }
      s"$before$realPort$link"
    }
  }

  override def content(url: String) = {
    catching(classOf[Exception]) either extractContent(url) fold(
      err => err,
      ok => Right(ok)
    )
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
