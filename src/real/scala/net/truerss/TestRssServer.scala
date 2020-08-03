package net.truerss

import java.util.concurrent.atomic.AtomicInteger

import akka.http.scaladsl.model.{HttpCharsets, HttpEntity, HttpResponse, MediaTypes, StatusCodes}
import akka.http.scaladsl.server.Directives._

case class TestRssServer(host: String, port: Int) {

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

  val contentOk = HttpResponse(
    status = StatusCodes.OK,
    entity = HttpEntity(
      MediaTypes.`text/html` withCharset HttpCharsets.`UTF-8`,
      content
    )
  )

  private def sendRssFeed(content: String) = {
    HttpResponse(
      status = StatusCodes.OK,
      entity = HttpEntity(
        MediaTypes.`application/xml` withCharset HttpCharsets.`UTF-8`,
        content
      )
    )
  }

  val route = pathPrefix("rss" | "rss1" | "rss2") {
    get {
      complete {
        p("rss")
        rssStats.incrementAndGet()
        if (shouldProduceNewEntities) {
          sendRssFeed(rss1)
        } else {
          sendRssFeed(rss)
        }
      }
    }
  } ~ pathPrefix("error-rss") {
    get {
      complete {
        if (shouldProduceErrors) {
          sendRssFeed(rss).copy(status = StatusCodes.InternalServerError)
        } else {
          sendRssFeed(rss)
        }
      }
    }
  } ~ pathPrefix("boom") {
    get {
      complete(contentOk.copy(status = StatusCodes.InternalServerError))
    }
  } ~ pathPrefix("content") {
    pathPrefix("ok") {
      complete(contentOk)
    } ~ pathPrefix("test") {
      complete(contentOk)
    } ~ pathPrefix("boom") {
      complete(contentOk.copy(status = StatusCodes.InternalServerError))
    }
  } ~ pathPrefix("not-rss") {
    complete {
      p("not rss")
      notRssStats.incrementAndGet()
      HttpResponse(
        status = StatusCodes.OK,
        entity = HttpEntity(
          MediaTypes.`text/css` withCharset HttpCharsets.`UTF-8`,
          rss
        )
      )
    }
  }

  private def p(x: String) = {
    println(s"=============== $x ===============")
  }

}
