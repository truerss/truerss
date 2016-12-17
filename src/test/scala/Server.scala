/*
import akka.actor.Actor

import scala.concurrent.duration.Duration
import spray.routing.HttpService

object RssHelper {

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

trait BaseServer extends HttpService {

  import RssHelper._

  val route = {
    path("ok-rss") {
      complete {
        rss.toString
      }
    } ~ path("bad-rss") {
      complete("foo")
    } ~ path("content1") {
      complete(content.toString)
    } ~ path("content2") {
      complete("foo")
    }
  }




}

class Server extends Actor with BaseServer {
  def actorRefFactory = context
  val receive = runRoute(route)
}

*/
