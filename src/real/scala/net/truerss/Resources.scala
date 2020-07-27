package net.truerss

import java.net.ServerSocket
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import truerss.util.TrueRSSConfig

trait Resources {

  private val allocated = scala.collection.mutable.ArrayBuffer[ServerSocket]()

  def allocatePort: Int = {
    val server = new ServerSocket(0)
    val port = server.getLocalPort
    server.close()
    allocated += server
    port
  }

  def suiteName: String

  implicit val system: ActorSystem = ActorSystem(suiteName)

  val port = allocatePort

  val host = "localhost"

  val wsPort = allocatePort

  val serverPort = allocatePort

  def url: String = s"http://$host:$port"
  def wsUrl: String = s"ws://$host:$wsPort"
  def rssUrl: String = s"http://$host:$serverPort/rss"
  def rssUrl1: String = s"http://$host:$serverPort/rss1"
  def rssUrlWithError: String = s"http://$host:$serverPort/error-rss"

  def sleep() = Thread.sleep(1000)

  val actualConfig = TrueRSSConfig().copy(
    host = host,
    port = port,
    wsPort = wsPort
  )

  println(s" [$suiteName] ==========> port=$port, wsPort=$wsPort, serverPort=$serverPort")

  private val server = TestRssServer(host, serverPort)

  def getRssStats: Int = server.rssStats.intValue()

  def produceNewEntities: Unit = server.produceNewEntities()

  def produceErrors: Unit = server.produceErrors()

  def content: String = server.content

  def opmlFile: String = Resources.load("test.opml", host, serverPort)

  protected var wsClient: WSClient = _

  def startServer() = {
    Http()(system).bindAndHandle(
      server.route,
      host,
      serverPort
    ).foreach { _ =>
      println(s"=============> run test server on: $host:$serverPort")
    }(system.dispatcher)
  }

  def startWsClient(): Unit = {
    wsClient = new WSClient(wsUrl)
    wsClient.connectBlocking(3, TimeUnit.SECONDS)
    connectToWs()
  }

  private def connectToWs(): Boolean = {
    if (!wsClient.isOpen) {
      wsClient.reconnectBlocking()
      connectToWs()
    } else {
      true
    }
  }

  def shutdown() = {
    wsClient.closeBlocking()
    allocated.foreach(_.close())
    Http().shutdownAllConnectionPools().foreach { _ =>
      system.terminate()
    }(system.dispatcher)
  }

}

object Resources {
  def load(file: String, host: String, port: Int): String = {
    scala.io.Source.fromInputStream(
      getClass.getClassLoader.getResourceAsStream(file)
    ).mkString
      .replaceAll("%%HOST%%", host)
      .replaceAll("%%PORT%%", s"$port")
  }
}
