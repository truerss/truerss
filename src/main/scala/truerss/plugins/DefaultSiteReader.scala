package truerss.plugins

import java.io.ByteArrayInputStream
import java.util.Date

import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.io.{SyndFeedInput, XmlReader}
import org.jsoup.Jsoup
import org.jsoup.parser._

import scala.collection.JavaConversions._
import scala.util.control.Exception._
import com.rometools.rome.io.ParsingFeedException

class DefaultSiteReader(config: Map[String, String]) extends BasePlugin(config) {

  import truerss.util.Request._

  override val author = "fntz"
  override val about = "default rss|atom reader"
  override val pluginName = "Default"
  override val version = "0.0.3"

  val sfi = new SyndFeedInput()

  override def matchUrl(url: String) = true

  override def newEntries(url: String) = {
    catching(classOf[Exception]) either extract(url) fold(
      err => Left(err.getMessage),
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
      err => Left(err.getMessage),
      ok => Right(ok)
    )
  }

  private def extractContent(url: String) = {
    val response = getResponse(url)

    if (response.isError) {
      throw new RuntimeException(s"Connection error for ${url}") 
    }

    val doc = Jsoup.parse(response.body.toString).select("article")

    if (doc.size == 0) {
      None
    } else {
      doc.select("input, textarea, script").remove()
      Some(doc.toString)
    }
  }



}
