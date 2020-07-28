package net.truerss

import net.truerss.tests.AllTestsTogether
import org.specs2.specification.BeforeAfterAll
import org.testcontainers.containers.MySQLContainer
import truerss.AppRunner
import truerss.util.DbConfig

trait MySqlTests extends AllTestsTogether with BeforeAfterAll with Resources {

  override def suiteName: String = "mysql-tests"

  val container = new MySQLContainer()

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
    AppRunner.run(actualConfig, dbConf, isUserConf)(system)
    startWsClient()
  }

  override def afterAll(): Unit = {
    shutdown()
    container.stop()
  }
}
