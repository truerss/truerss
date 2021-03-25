package net.truerss

import com.github.fntz.omhs.OMHSServer
import net.truerss.tests.AllTestsTogether
import org.specs2.specification.BeforeAfterAll
import org.testcontainers.containers.PostgreSQLContainer
import truerss.AppRunner
import truerss.util.DbConfig

class PostgresTests extends AllTestsTogether with BeforeAfterAll with Resources {

  override def suiteName: String = "posgtres-tests"

  val container = new PostgreSQLContainer("postgres:9.6.12")

  private var appServer: OMHSServer.Instance = _

  override def beforeAll(): Unit = {
    container.start()
    println(container.getJdbcUrl)
    startServer()
    val isUserConf = false
    val dbConf = new DbConfig(
      dbBackend = "postgresql",
      dbHost = container.getHost,
      dbPort = container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT).toString,
      dbName = container.getDatabaseName,
      dbUsername = container.getUsername,
      dbPassword = container.getPassword
    )
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
