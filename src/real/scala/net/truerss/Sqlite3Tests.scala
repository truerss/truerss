package net.truerss

import java.io.File
import java.nio.file.Files

import org.specs2.specification.BeforeAfterAll
import truerss.AppRunner
import truerss.util.DbConfig

import scala.concurrent.Await
import scala.concurrent.duration._

class Sqlite3Tests
  extends AllTestsTogether
    with BeforeAfterAll with Resources {

  val isUserConf = true
  val dbConf = new DbConfig(
    dbBackend = "sqlite",
    dbHost = "",
    dbPort = "",
    dbName = "sqlite-test.tdb",
    dbUsername = "",
    dbPassword = ""
  )

  override def beforeAll(): Unit = {
    AppRunner.run(actualConfig, dbConf, isUserConf)(system)
  }

  override def afterAll(): Unit = {
    Await.result(system.terminate(), 3 seconds)
    val file = new File(s"./${dbConf.dbName}")
    Files.deleteIfExists(file.toPath)
  }

  override def suiteName: String = "sqlite-tests"
}