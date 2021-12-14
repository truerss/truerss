package net.truerss.plugins

import org.specs2.mutable.Specification
import truerss.dto.EnclosureDto
import truerss.plugins.{AtomParser, RSSParser}

class FeedReaderTests extends Specification {

  "parser" should {
    "rss" in {
      val result = RSSParser.parse(HtmlFixtures.rss)
      result should have size 3

      result.flatMap(_.title).toVector ==== Vector(
        "Brains Sweep Themselves Clean of Toxins During Sleep (2013)",
        "Memory Efficient Hard Real-Time Garbage Collection [pdf]",
        "The US digital service"
      )
      result.foreach { x =>
        x.title must beSome
        x.url must beSome
        x.author must beNone
        x.description must beSome
      }

      val enclosures = result.flatMap(_.enclosure)
      enclosures must have size 1

      enclosures.head ==== EnclosureDto(
        `type` = "video/wmv",
        url = "https://www.w3schools.com/media/3d.wmv",
        length = 78645
      )

      result.flatMap(_.url).toVector should have size 3
    }

    "atom" in {
      val result = AtomParser.parse(scala.xml.XML.loadString(HtmlFixtures.atom))

      result should have size 3

      result.flatMap(_.title).toVector ==== Vector(
        "Atom-Powered Robots Run Amok",
        "Atom-Powered Robots Run Amok#1",
        "title123"
      )
      result.foreach { x =>
        x.title must beSome
        x.url must beSome
        x.author must beSome
        x.description must beSome
        x.enclosure must beNone
      }

      result.flatMap(_.author).toVector.distinct ==== Vector(
        "Local",
        "John Doe"
      )

      result.flatMap(_.url).toVector ==== Vector(
        "http://example.org/2003/12/13/atom",
        "http://example.org/2003/12/13/atom03",
        "http://example.org/2003/12/13/atom01"
      )
    }
  }

}
