package net.truerss

import java.io.File
import java.nio.file.Files
import net.truerss.tests.AllTestsTogether
import org.specs2.specification.BeforeAfterAll
import truerss.db.DbLayer
import truerss.{AppInstance, AppRunner}
import truerss.util.DbConfig

class Sqlite3Tests
  extends AllTestsTogether
    with BeforeAfterAll with Resources {

  override def suiteName: String = "sqlite-tests"

  private val dbName = "sqlite-test.tdb"
  private var appServer: AppInstance = _
  override protected def dbLayer: DbLayer = appServer.dbLayer

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