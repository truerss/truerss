package net.truerss

import java.io.File
import java.nio.file.Files

import net.truerss.tests.AllTestsTogether
import org.specs2.specification.BeforeAfterAll
import truerss.AppRunner
import truerss.util.DbConfig

class Sqlite3Tests
  extends AllTestsTogether
    with BeforeAfterAll with Resources {

  override def suiteName: String = "sqlite-tests"

  private val dbName = "sqlite-test.tdb"

  override def beforeAll(): Unit = {
    startServer()
    val isUserConf = true
    val dbConf = new DbConfig(
      dbBackend = "sqlite",
      dbHost = "",
      dbPort = "",
      dbName = dbName,
      dbUsername = "",
      dbPassword = ""
    )
    AppRunner.run(actualConfig, dbConf, isUserConf)(system)
    startWsClient()
  }

  override def afterAll(): Unit = {
    shutdown()
    val file = new File(s"./$dbName")
    Files.deleteIfExists(file.toPath)
  }
}