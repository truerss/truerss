package net.truerss

import com.github.fntz.omhs.OMHSServer

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
  private var appServer: OMHSServer.Instance = _

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
    appServer = AppRunner.run(actualConfig, dbConf, isUserConf)(system)
    appServer.start()
    startWsClient()
  }

  override def afterAll(): Unit = {
    shutdown()
    appServer.stop()
    val file = new File(s"./$dbName")
    Files.deleteIfExists(file.toPath)
  }
}