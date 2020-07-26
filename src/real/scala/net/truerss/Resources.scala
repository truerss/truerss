package net.truerss

import java.net.ServerSocket

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import truerss.util.TrueRSSConfig

trait Resources {

  def allocatePort: Int = {
    val socket = new ServerSocket(0)
    socket.getLocalPort
  }

  def suiteName: String

  implicit val system: ActorSystem = ActorSystem(suiteName)

  val port = allocatePort

  val host = "localhost"

  val wsPort = allocatePort

  val serverPort = allocatePort

  println("@"*100 + s"---> ${url}")

  def url: String = s"http://$host:$port"
  def rssUrl: String = s"http://$host:$serverPort/rss"
  def rssUrl1: String = s"http://$host:$serverPort/rss1"

  def sleep = Thread.sleep(1000)

  val actualConfig = TrueRSSConfig().copy(
    host = host,
    port = port,
    wsPort = wsPort
  )

  println(s"==========> port=$port, wsPort=$wsPort, serverPort=$serverPort")

  println(s"current server url: =============> ${actualConfig.url}")

  private val server = TestRssServer(host, serverPort)

  def getRssStats: Int = server.rssStats.intValue()

  def produceNewEntities: Unit = server.produceNewEntities()

  def content: String = server.content

  def opmlFile: String = Resources.load("test.opml", host, serverPort)

  Http()(system).bindAndHandle(
    server.route,
    host,
    serverPort
  ).foreach { _ =>
    println(s"=============> run test server on: $host:$serverPort")
  }(system.dispatcher)


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
