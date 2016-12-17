
import java.net.{ServerSocket, URI, URL}

import akka.actor._
import akka.io.IO
import akka.pattern._
import akka.util.Timeout
import com.github.truerss.base.ContentTypeParam
import com.github.truerss.base.ContentTypeParam.UrlRequest
import com.github.truerss.base.Errors.UnexpectedError
import com.github.truerss.base.Errors.ParsingError
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import truerss.plugins.DefaultSiteReader

import scala.concurrent.Await
import scala.concurrent.duration._



class DefaultReaderTest extends Specification {

  implicit val timeout = Timeout(10 seconds)



  val url = s"http://localhost:10000"
  val okRss = s"$url/ok-rss"
  val badRss = s"${url}/bad-rss"
  val failUrl = s"http://localhost:10001"
  val content1Url = s"$url/content1"
  val content2Url = s"$url/content2"

  val defaultReader = new DefaultSiteReader(ConfigFactory.empty)

  "matchUrl" should {
    "match any url" in {
      defaultReader.matchUrl(new URL(url)) must beTrue
      defaultReader.matchUrl(new URL("https://www.youtube.com")) must beTrue
      defaultReader.matchUrl(new URL("https://news.ycombinator.com/")) must beTrue
    }
  }

//  "newEntries" should {
//    "return new entries when parse valid rss or atom" in {
//      val result = defaultReader.newEntries(okRss)
//      result.isRight should be(true)
//      val entries = result.right.get
//      entries should have size 3
//      val need = Seq(
//        "Brains Sweep Themselves Clean of Toxins During Sleep (2013)",
//        "Memory Efficient Hard Real-Time Garbage Collection [pdf]",
//        "The US digital service"
//      ).map { title =>
//        if (title.length > 42) {
//          title.substring(0, 42)
//        } else {
//          title
//        }
//      }
//      entries.map(_.title) should contain allOf (
//        need.head, need(1), need(2)
//      )
//    }
//
//    "return error when parse failed" in {
//      val result = defaultReader.newEntries(badRss)
//      result.isLeft should be(true)
//      result.left.get should be(UnexpectedError(
//        "Content is not allowed in prolog.")
//      )
//    }
//
//    "return error when connection failed" in {
//      val result = defaultReader.newEntries(failUrl)
//      result.isLeft should be(true)
//    }
//  }
//
  "Content" should {
    "extract content" in {
      val result = defaultReader.content(UrlRequest(new URL(content1Url)))
      result.isRight should beTrue
      result.right.get.get must contain("The US digital service")
    }
  }

}

trait HtmlFixtures {
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

  val content = <div>
    <article>
      The US digital service
    </article>
  </div>
}

