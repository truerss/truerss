package truerss.plugins

import truerss.dto.EnclosureDto

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import truerss.util.CommonImplicits

import scala.util.Try
import scala.xml.{Elem, Node}

trait FeedParser {
  import CommonImplicits._

  def from(tagName: String)(implicit source: Node): Option[String] = {
    val x = source \ tagName
    Option.unless(x.isEmpty)(x.text)
  }
  def parse(x: Elem): Iterable[EntryDto]

  def getDate(x: String)(implicit format: DateTimeFormatter): Option[Date] = {
    Try(LocalDateTime.parse(x, format)).toOption.map(_.toDate)
  }

  def getEnclosure(implicit node: Node): Option[EnclosureDto]
}

case object FeedParser {
  def matchParser(x: Elem): FeedParser = {
    if (x.label == "RDF" || x.label == "rss") {
      RSSParser
    } else {
      AtomParser
    }
  }
}

case object RSSParser extends FeedParser {
  val _title = "title"
  val _link = "link"
  val _description = "description"
  val _item = "item"
  val _pubDate = "pubDate"
  val _author = "author"
  val _enclosure = "enclosure"

  implicit val format: DateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME

  override def parse(x: Elem): Iterable[EntryDto] = {
    (x \\ _item).map { implicit item =>
      EntryDto(
        title = from(_title),
        url = from(_link),
        description = from(_description),
        publishedDate = from(_pubDate).flatMap(getDate).getOrElse(new java.util.Date),
        author = from(_author),
        enclosure = getEnclosure
      )
    }
  }

  override def getEnclosure(implicit node: Node): Option[EnclosureDto] = {
    for {
      n <- (node \\ _enclosure).headOption
      tp <- n.attribute("type").map(_.text)
      url <- n.attribute("url").map(_.text)
      length <- n.attribute("length").flatMap(_.text.toIntOption)
    } yield {
      EnclosureDto(
        `type` = tp,
        url = url,
        length = length
      )
    }
  }
}

case object AtomParser extends FeedParser {
  val _author = "author"
  val _name = "name"
  val _link = "link"
  val _updated = "updated"
  val _title = "title"
  val _entry = "entry"
  val _summary = "summary"

  protected def getAuthors(x: Node): Option[String] = {
    val r = x \ _author
    if (r.isEmpty) {
      None
    } else {
      Some(r.flatMap(e => (e \ _name).map(_.text)).mkString(", "))
    }
  }

  protected def getLinks(x: Node): Option[String] = {
    val links = x \ _link
    val r = links
      .filter(_.attribute("rel").exists(_.forall(_.text == "alternate")))
      .flatMap(_.attribute("href").map(_.text)).headOption

    if (links.nonEmpty && r.isEmpty) {
      links.headOption.flatMap(_.attribute("href").map(_.text))
    } else {
      r
    }
  }

  implicit val format: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

  override def parse(x: Elem): Iterable[EntryDto] = {
    // global author
    val xs = x \ _author
    val g = if (xs.nonEmpty) {
      (xs \ _name).headOption.map(_.text).orElse(Some(xs.text))
    } else {
      None
    }

    (x \ _entry).map { implicit entry =>
      EntryDto(
        url = getLinks(entry),
        title = from(_title),
        author = getAuthors(entry).orElse(g),
        publishedDate = from(_updated).flatMap(getDate).getOrElse(new java.util.Date),
        description = from(_summary),
        enclosure = getEnclosure
      )
    }
  }

  // TODO: need to find example of enclosure in Atom
  override def getEnclosure(implicit node: Node): Option[EnclosureDto] = None
}