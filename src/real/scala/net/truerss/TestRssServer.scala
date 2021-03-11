package net.truerss

import java.util.concurrent.atomic.AtomicInteger
import com.github.fntz.omhs.{AsyncResult, CommonResponse, Route, RoutingDSL}
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.util.CharsetUtil

case class TestRssServer(host: String, port: Int) {

  import RoutingDSL._
  import AsyncResult.Implicits._

  val rssStats = new AtomicInteger(0)
  val notRssStats = new AtomicInteger(0)

  @volatile private var shouldProduceNewEntities = false
  @volatile private var shouldProduceErrors = false

  def produceNewEntities(): Unit = {
    p("Switch to new mode [push new entities]")
    shouldProduceNewEntities = true
  }

  def produceErrors(): Unit = {
    shouldProduceErrors = true
  }

  private def loadRss(file: String): String = {
    Resources.load(file, host, port)
  }

  val rss = loadRss("rss.xml")

  val rss1 = loadRss("rss1.xml")

  val content = "<div>test-content</div>"

  val contentOk = CommonResponse(
    status = HttpResponseStatus.OK,
    contentType = "text/html;charset=utf-8",
    content.getBytes(CharsetUtil.UTF_8)
  )

  private def sendRssFeed(content: String) = {
    CommonResponse(
      status = HttpResponseStatus.OK,
      contentType = "application/xml",
      content.getBytes(CharsetUtil.UTF_8)
    )
  }

  def send: AsyncResult = {
    p("rss")
    rssStats.incrementAndGet()
    if (shouldProduceNewEntities) {
      sendRssFeed(rss1)
    } else {
      sendRssFeed(rss)
    }
  }

  val route1 = get("rss") ~> {() => send}
  val route2 = get("rss1") ~> {() => send}
  val route3 = get("rss3") ~> {() => send}
  val route4 = get("error-rss") ~> {() =>
    if (shouldProduceErrors) {
      sendRssFeed(rss).copy(status = HttpResponseStatus.INTERNAL_SERVER_ERROR)
    } else {
      sendRssFeed(rss)
    }
  }
  val route5 = get("boom") ~> {() =>
    contentOk
      .copy(status = HttpResponseStatus.INTERNAL_SERVER_ERROR)
  }
  val route6 = get("content" / "ok") ~> {() => contentOk }
  val route7 = get("content" / "test") ~> {() => contentOk}
  val route8 = get("content" / "boom") ~> {() =>
    contentOk
      .copy(status = HttpResponseStatus.INTERNAL_SERVER_ERROR)
  }
  val route9 = get("not-rss") ~> {() =>
    p("not rss")
    notRssStats.incrementAndGet()
    CommonResponse.plain(rss)
  }

  val route = new Route() :: route1 :: route2 :: route3 :: route4 ::
    route5 :: route6 :: route7 :: route8 :: route9

  private def p(x: String) = {
    println(s"=============== $x ===============")
  }

}
