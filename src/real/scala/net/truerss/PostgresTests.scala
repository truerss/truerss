package net.truerss

import net.truerss.tests.AllTestsTogether
import org.specs2.specification.BeforeAfterAll
import org.testcontainers.containers.PostgreSQLContainer
import truerss.AppRunner
import truerss.util.DbConfig

class PostgresTests extends AllTestsTogether with BeforeAfterAll with Resources {

  override def suiteName: String = "posgtres-tests"

  val container = new PostgreSQLContainer()

  override def beforeAll(): Unit = {
    container.start()
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
    AppRunner.run(actualConfig, dbConf, isUserConf)(system)
    startWsClient()
  }

  override def afterAll(): Unit = {
    shutdown()
    container.stop()
  }

}
