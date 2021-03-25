package net.truerss

import com.github.fntz.omhs.OMHSServer
import net.truerss.tests.AllTestsTogether
import org.specs2.specification.BeforeAfterAll
import org.testcontainers.containers.MySQLContainer
import truerss.AppRunner
import truerss.util.DbConfig

class MySqlTests extends AllTestsTogether with BeforeAfterAll with Resources {

  override def suiteName: String = "mysql-tests"

  val container = new MySQLContainer("mysql:8.0.23")


  private var appServer: OMHSServer.Instance = _

  override def beforeAll(): Unit = {
    container.start()
    startServer()
    val isUserConf = false
    val dbConf = new DbConfig(
      dbBackend = "mysql",
      dbHost = container.getHost,
      dbPort = container.getMappedPort(MySQLContainer.MYSQL_PORT).toString,
      dbName = container.getDatabaseName,
      dbUsername = container.getUsername,
      dbPassword = container.getPassword
    )
    println(s"------> ${container.getJdbcUrl}")
    appServer = AppRunner.run(actualConfig, dbConf, isUserConf)(system)
    appServer.start()
    startWsClient()
  }

  override def afterAll(): Unit = {
    shutdown()
    appServer.stop()
    container.stop()
  }
}
