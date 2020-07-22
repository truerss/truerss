package truerss.db.driver

import java.nio.file.Paths
import java.util.Properties

import slick.jdbc._
import truerss.util.DbConfig

trait DBProfile {
  val profile: JdbcProfile
  val driver: String
  val sourceClassName: String

  val defaultConnectionSize: Int = 10

  def props(dbConf: DbConfig, isUserConf: Boolean): Properties = {
    val props = new Properties()
    props.setProperty("dataSourceClassName", sourceClassName)
    props.setProperty("dataSource.user", dbConf.dbUsername)
    props.setProperty("dataSource.password", dbConf.dbPassword)
    props.setProperty("dataSource.databaseName", dbConf.dbName)
    props.setProperty("dataSource.serverName", dbConf.dbHost)
    props.setProperty("dataSource.portNumber", dbConf.dbPort)
    props
  }

}

object DBProfile {

  private val dbMap = Map(
    "postgresql" -> Postgresql,
    "sqlite" -> Sqlite,
    "mysql" -> Mysql
  )

  def get(x: String): Option[SupportedDb] = {
    dbMap.get(x.toLowerCase)
  }

  def create(db: SupportedDb) = {
    db match {
      case Postgresql => new DBProfile {
        override val driver: String = "org.postgresql.Driver"
        override val profile: JdbcProfile = PostgresProfile
        override val sourceClassName: String = "org.postgresql.ds.PGSimpleDataSource"
      }

      case Sqlite => new DBProfile {
        override val profile: JdbcProfile = SQLiteProfile
        override val driver = "org.sqlite.JDBC"
        override val sourceClassName: String = "org.sqlite.SQLiteDataSource"
        override val defaultConnectionSize: Int = 1 // for sqlite: need to avoid locks

        private val sqliteUrl = "jdbc:sqlite:"

        override def props(dbConf: DbConfig, isUserConf: Boolean): Properties = {
          val dbName = if (isUserConf) {
            dbConf.dbName
          } else {
            s"${Paths.get("").toAbsolutePath}/${dbConf.dbName}"
          }
          val props = new Properties()
          props.setProperty("dataSource.databaseName", dbName)
          props.setProperty("driverClassName", driver)
          val jdbcUrl = if (dbName.startsWith(sqliteUrl)) {
            dbName
          } else {
            s"$sqliteUrl$dbName"
          }
          props.setProperty("jdbcUrl", jdbcUrl)
          props
        }
      }

      case Mysql => new DBProfile {
        override val driver: String = "com.mysql.jdbc.Driver"
        override val profile: JdbcProfile = MySQLProfile
        override val sourceClassName: String = "com.mysql.jdbc.jdbc2.optional.MysqlDataSource"
      }
    }
  }

}


