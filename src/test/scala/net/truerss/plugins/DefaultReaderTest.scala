package net.truerss.plugins

import java.net.URL

import akka.util.Timeout
import com.github.truerss.base.ContentTypeParam.UrlRequest
import com.github.truerss.base.Errors.UnexpectedError
import com.typesafe.config.ConfigFactory
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scalaj.http.HttpResponse
import truerss.plugins.DefaultSiteReader

import scala.concurrent.duration._

class DefaultReaderTest extends Specification with Mockito {

  implicit val timeout = Timeout(10 seconds)

  val url = s"http://example.com"
  val okRss = s"$url/ok-rss"
  val badRss = s"$url/bad-rss"
  val failUrl = s"http://faile-example.com"
  val content1Url = s"$url/content1"
  val content2Url = s"$url/content2"

  "matchUrl" should {
    "match any url" in new ReaderScope(HtmlFixtures.content) {
      defaultReader.matchUrl(new URL(url)) must beTrue
      defaultReader.matchUrl(new URL("https://www.youtube.com")) must beTrue
      defaultReader.matchUrl(new URL("https://news.ycombinator.com/")) must beTrue
    }
  }

  "newEntries" should {
    "return new entries when parse valid rss or atom" in new ReaderScope(HtmlFixtures.rss.toString) {
      val result = defaultReader.newEntries(okRss)
      result.isRight must beTrue
      val entries = result.right.get
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
      result.left.get must haveClass[UnexpectedError]
    }

    "return error when connection failed" in new ReaderScope("foo", 503) {
      val result = defaultReader.newEntries(failUrl)
      result.isLeft must beTrue
    }
  }

  "Content" should {
    "extract content" in new ReaderScope(HtmlFixtures.content) {
      val result = defaultReader.content(UrlRequest(new URL(content1Url)))
      result.isRight must beTrue
      result.right.get.get must contain(HtmlFixtures.text)
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

  val content = s"""<div>
    <article>
      $text
    </article>
  </div>"""
}
