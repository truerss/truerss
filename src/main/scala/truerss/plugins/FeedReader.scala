package truerss.plugins

import truerss.dto.EnclosureDto

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import truerss.util.CommonImplicits

import scala.util.Try
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.{Elem, Node}

trait FeedParser {
  import CommonImplicits._

  def parse(x: Elem): Iterable[EntryDto]

  protected val tags: Vector[String] = Vector.empty

  protected def from(tagName: String)(implicit source: Node): Option[String] = {
    val x = source \ tagName
    Option.unless(x.isEmpty)(x.text)
  }

  protected def getDate(x: String)(implicit format: DateTimeFormatter): Option[Date] = {
    Try(LocalDateTime.parse(x, format)).toOption.map(_.toDate)
  }

  protected def getEnclosure(node: Node): Option[EnclosureDto]

  protected def toMap(entry: Node): Map[String, Node] = {
    entry.child.filter(x => tags.contains(x.label))
      .map(x => x.label -> x)
      .toMap
  }
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
  private val _title = "title"
  private val _link = "link"
  private val _description = "description"
  private val _item = "item"
  private val _pubDate = "pubDate"
  private val _author = "author"
  private val _enclosure = "enclosure"

  implicit private val format: DateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME

  override protected val tags = Vector(_title, _link, _description, _pubDate, _author, _enclosure)

  override def parse(x: Elem): Iterable[EntryDto] = {
    val now = new java.util.Date
    (x \\ _item).map { implicit item =>
      val children = toMap(item)
      def get(attr: String) = children.get(attr).map(_.text)
      EntryDto(
        title = get(_title),
        url = get(_link),
        description = get(_description),
        publishedDate = get(_pubDate).flatMap(getDate).getOrElse(now),
        author = get(_author),
        enclosure = children.get(_enclosure).flatMap(getEnclosure)
      )
    }
  }

  override protected def getEnclosure(node: Node): Option[EnclosureDto] = {
    for {
      tp <- node.attribute("type").map(_.text)
      url <- node.attribute("url").map(_.text)
      length <- node.attribute("length").flatMap(_.text.toIntOption)
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
  private val _author = "author"
  private val _name = "name"
  private val _link = "link"
  private val _updated = "updated"
  private val _title = "title"
  private val _entry = "entry"
  private val _summary = "summary"

  implicit private val format: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

  override protected val tags = Vector(_updated, _title, _summary, _link, _author)

  override def parse(x: Elem): Iterable[EntryDto] = {
    // global author
    val xs = x \ _author \ _name
    val globalAuthor = if (xs.nonEmpty) {
      xs.headOption.map(_.text)
    } else {
      None
    }

    (x \ _entry).map { implicit entry =>
      val children = toMap(entry)
      def get(attr: String) = {
        children.get(attr) match {
          case Some(node) if attr == _link =>
            getLinks(node)
          case Some(node) if attr == _author =>
            getAuthors(node)
          case Some(node) =>
            Some(node.text)
          case _ =>
            None
        }
      }

      EntryDto(
        url = get(_link),
        title = get(_title),
        author = get(_author).orElse(globalAuthor),
        publishedDate = get(_updated).flatMap(getDate).getOrElse(new java.util.Date),
        description = get(_summary),
        enclosure = getEnclosure(entry)
      )
    }
  }

  private def getAuthors(x: Node): Option[String] = {
    val xs = (x \ _name).map(_.text)
    if (xs.nonEmpty) {
      Some(xs.mkString(", "))
    } else {
      None
    }
  }

  protected def getLinks(links: Node): Option[String] = {
    val r = links
      .filter(_.attribute("rel").exists(_.forall(_.text == "alternate")))
      .flatMap(_.attribute("href").map(_.text)).headOption

    if (links.nonEmpty && r.isEmpty) {
      links.headOption.flatMap(_.attribute("href").map(_.text))
    } else {
      r
    }
  }

  // TODO: need to find example of enclosure in Atom
  override def getEnclosure(node: Node): Option[EnclosureDto] = None
}