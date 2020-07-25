package net.truerss.sqlite3

import java.io.File
import java.nio.file.Files

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import net.truerss.{Resources, TestRssServer}
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import truerss.AppRunner
import truerss.util.{DbConfig, TrueRSSConfig}

import scala.concurrent.Await
import scala.concurrent.duration._

class Sqlite3Tests
  extends Specification
    with BeforeAfterAll
  //  with PluginsApiTests
//    with SettingsApiTests
    with FullTests
    with Resources {

  override def port: Int = 10000

  override def host: String = "localhost"

  override def wsPort: Int = 10001

  override def serverPort: Int = 10002

  val isUserConf = true
  val dbConf = new DbConfig(
    dbBackend = "sqlite",
    dbHost = "",
    dbPort = "",
    dbName = "sqlite-test.tdb",
    dbUsername = "",
    dbPassword = ""
  )
  val actualConfig = TrueRSSConfig().copy(
    host = host,
    port = port,
    wsPort = wsPort
  )

  println(s"current server url: =============> ${actualConfig.url}")

  override implicit val system = ActorSystem("sqlite3-tests")

  private val server = TestRssServer(host, serverPort)

  override def getRssStats: Int = server.rssStats.intValue()

  override def produceNewEntities: Unit = server.produceNewEntities()

  override def content: String = server.content

  override def opmlFile: String = Resources.load("test.opml", host, serverPort)

  Http()(system).bindAndHandle(
    server.route,
    host,
    serverPort
  ).foreach { _ =>
    println(s"=============> run test server on: $host:$serverPort")
  }(system.dispatcher)

  override def beforeAll(): Unit = {
    AppRunner.run(actualConfig, dbConf, isUserConf)(system)
  }

  override def afterAll(): Unit = {
    Await.result(system.terminate(), 3 seconds)
    val file = new File(s"./${dbConf.dbName}")
    Files.deleteIfExists(file.toPath)
  }
}