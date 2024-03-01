package net.truerss.plugins

import akka.util.Timeout
import com.github.truerss.base.ContentTypeParam.UrlRequest
import com.github.truerss.base.Errors.UnexpectedError
import com.typesafe.config.ConfigFactory
import org.jsoup.Jsoup
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scalaj.http.HttpResponse
import truerss.plugins.DefaultSiteReader

import java.net.URI
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

class DefaultReaderTests extends Specification {

  implicit val timeout: Timeout = Timeout(10.seconds)

  val url = s"http://example.com"
  val okRss = s"$url/ok-rss"
  val badRss = s"$url/bad-rss"
  val failUrl = s"http://faile-example.com"
  val content1Url = s"$url/content1"
  val content2Url = s"$url/content2"

  "matchUrl" should {
    "match any url" in new ReaderScope(HtmlFixtures.content) {
      defaultReader.matchUrl(URI.create(url)) must beTrue
      defaultReader.matchUrl(URI.create("https://www.youtube.com")) must beTrue
      defaultReader.matchUrl(URI.create("https://news.ycombinator.com/")) must beTrue
    }
  }

  "normalize links" should {
    "normalize" in {
      val link = "https://example.com"
      DefaultSiteReader.normalizeLink(link, link) ==== link

      DefaultSiteReader.normalizeLink(link, s"$link/test") ==== s"$link/test"

      DefaultSiteReader.normalizeLink(link, s"/test") ==== s"$link/test"
    }
  }

  "newEntries" should {
    "return new entries when parse valid rss or atom" in new ReaderScope(HtmlFixtures.rss.toString) {
      val result = defaultReader.newEntries(okRss)
      result.isRight must beTrue
      val entries = result.getOrElse(Vector.empty)
      entries must have size 3
      val need = Seq(
        "Brains Sweep Themselves Clean of Toxins During Sleep (2013)",
        "Memory Efficient Hard Real-Time Garbage Collection [pdf]",
        "The US digital service"
      )

      entries.map(_.title) must contain(allOf(need : _*))
    }

    "return error when parse failed" in new ReaderScope("<foo>") {
      val result = defaultReader.newEntries(badRss)
      println(result)
      result.isLeft must beTrue
      result.swap.map { _ must haveClass[UnexpectedError] }
      success
    }

    "return error when connection failed" in new ReaderScope("foo", 503) {
      val result = defaultReader.newEntries(failUrl)
      result.isLeft must beTrue
    }
  }

  "Content" should {
    "extract content" in new ReaderScope(HtmlFixtures.content) {
      val result = defaultReader.content(UrlRequest(URI.create(content1Url)))
      result.isRight must beTrue
      result.map { _.get must contain(HtmlFixtures.text) }
      success
    }
    "#parseContent" in {
      val result = DefaultSiteReader.parseContent(url, HtmlFixtures.content)

      result must contain(HtmlFixtures.text)

      val asJsoup = Jsoup.parse(result)
      DefaultSiteReader.forbidElements.foreach { element =>
        asJsoup.select(element).asScala must be empty
      }

      asJsoup.select("a").asScala
        .head.attr("href") ==== s"$url${HtmlFixtures.link}"

      asJsoup.select("img").asScala
          .foreach { img =>
            img.attr("src").startsWith(url) must beTrue
            img.attr("src").endsWith(".png") must beTrue
          }

      success
    }
  }

  class ReaderScope(body: String, code: Int = 200) extends Scope {
    class MyTestReader extends DefaultSiteReader(ConfigFactory.empty) {
      override def getResponse(url: String) = new HttpResponse[String](
        body = body,
        code = code,
        headers = Map.empty
      )
    }

    val defaultReader = new MyTestReader
  }

}

object HtmlFixtures {
  val rss = <rss version="2.0">
    <channel>
      <title>Hacker News</title>
      <link>https://news.ycombinator.com/</link>
      <description>Links for the intellectually curious, ranked by readers.</description>
      <item>
        <title>Brains Sweep Themselves Clean of Toxins During Sleep (2013)</title>
        <link>http://www.npr.org/sections/health-shots/2013/10/18/236211811/brains-sweep-themselves-clean-of-toxins-during-sleep</link>
        <pubDate>Fri, 14 Aug 2015 18:21:06 +0000</pubDate>
        <comments>https://news.ycombinator.com/item?id=10061833</comments>
        <description>
          <![CDATA[<a href="https://news.ycombinator.com/item?id=10061833">Comments</a>]]>
        </description>
        <enclosure url="https://www.w3schools.com/media/3d.wmv" length="78645" type="video/wmv" />
      </item>
      <item>
        <title>Memory Efficient Hard Real-Time Garbage Collection [pdf]</title>
        <link>http://liu.diva-portal.org/smash/get/diva2:20899/FULLTEXT01.pdf</link>
        <pubDate>Sat, 15 Aug 2015 05:35:24 +0000</pubDate>
        <comments>https://news.ycombinator.com/item?id=10064445</comments>
        <description>
          <![CDATA[<a href="https://news.ycombinator.com/item?id=10064445">Comments</a>]]>
        </description>
      </item>
      <item>
        <title>The US digital service</title>
        <link>http://blog.samaltman.com/the-us-digital-service</link>
        <pubDate>Fri, 14 Aug 2015 16:00:49 +0000</pubDate>
        <comments>https://news.ycombinator.com/item?id=10060858</comments>
        <description>
          <![CDATA[<a href="https://news.ycombinator.com/item?id=10060858">Comments</a>]]>
        </description>
      </item>
    </channel>
  </rss>

  val text = "The US digital service"
  val img1 = "img1"
  val img2 = "img2"
  val link = "/link"

  val content = s"""<div>
    <article>
      <meta name="referrer" content="origin">
      <style type="text/css">
      p {
        color: #26b72b;
      }
      </style>
      $text
      <img id="$img1" src="http://example.com/1.png"/>
      <img id="$img2" src="/2.png"/>
      <a href="$link">test-link</a>
      <form><input type="text" value="asd" /></form>
      <input type="text" value="asd" />
      <script type="application/javascript">alert(1);</script>
    </article>
  </div>"""

  val atom = """<?xml version="1.0" encoding="utf-8"?>
    <feed xmlns="http://www.w3.org/2005/Atom">
      <title>Example Feed</title>
      <link href="http://example.org/"/>
      <updated>2003-12-13T18:30:02Z</updated>
      <author>
        <name>John Doe</name>
      </author>
      <id>urn:uuid:60a76c80-d399-11d9-b93C-0003939e0af6</id>
      <entry>
        <title>Atom-Powered Robots Run Amok</title>
        <link href="http://example.org/2003/12/13/atom"/>
        <id>urn:uuid:1225c695-cfb8-4ebb-aaaa-80da344efa6a</id>
        <updated>2003-12-13T18:30:02Z</updated>
        <summary>Some text.</summary>
        <author>
          <name>Local</name>
          <email>JohnDoe@example.com</email>
          <uri>http://example.com/~johndoe</uri>
        </author>
      </entry>
      <entry>
        <title>Atom-Powered Robots Run Amok#1</title>
        <link href="http://example.org/2003/12/13/atom03"/>
        <id>urn:uuid:1225c695-cfb8-4ebb-aaaa-80da344efadd</id>
        <updated>2004-12-13T18:30:02Z</updated>
        <summary>Some text#100.</summary>
      </entry>
      <entry>
        <title>title123</title>
        <link href="http://example.org/2003/12/13/atom01"/>
        <id>urn:uuid:1225c695-cfb8-4ebb-aaaa-80da344efadd</id>
        <updated>2004-10-13T18:30:02Z</updated>
        <summary>summary</summary>
      </entry>
    </feed>"""
}
