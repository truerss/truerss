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

        Entry(link, title.get, author, date, d, cont)
    }

    entries.toVector.reverse
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
    val base = s"${url0.getProtocol()}://${url0.getHost()}"

    val doc = Jsoup.parse(response.toString)
    val result = ContentExtractor.extract(doc.body())

    val need = doc.select(result.selector)
    need.select("img").foreach{ img =>
      val src = img.attr("src")
      val newSrc = if (src.startsWith("http")) {
        src
      } else if ((src.startsWith("/"))) {
        s"${base}${src}"
      } else {
        s"${base}/${src}"
      }

      img.attr("src", newSrc)
    }

    Some(doc.select(result.selector).html())
  }



}
