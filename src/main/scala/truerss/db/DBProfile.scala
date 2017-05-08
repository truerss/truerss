package truerss.db

import java.nio.file.Paths
import java.util.Properties

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import slick.jdbc._
import slick.jdbc.meta.MTable

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import truerss.util.DbConfig



sealed trait SupportedDb
case object H2 extends SupportedDb
case object Postgresql extends SupportedDb
case object Sqlite extends SupportedDb
case object Mysql extends SupportedDb

object SupportedDb {
  def load(dbConf: DbConfig, isUserConf: Boolean)(
          implicit ec: ExecutionContext
  ): DbLayer = {
    val backend: Option[SupportedDb] = DBProfile.get(dbConf.dbBackend)//Some(H2)

    if (backend.isEmpty) {
      Console.err.println(s"Unsupported database backend: ${dbConf.dbBackend}")
      sys.exit(1)
    }

    val dbProfile = DBProfile.create(backend.get)

    val db = backend.get match {
      case Sqlite =>
        val url = if (isUserConf) {
          s"jdbc:${dbConf.dbBackend}:/${dbConf.dbName}"
        } else {
          s"jdbc:${dbConf.dbBackend}:/${Paths.get("").toAbsolutePath}/${dbConf.dbName}"
        }
        JdbcBackend.Database.forURL(url, driver=dbProfile.driver)
      case H2 =>
        val url = "jdbc:h2:mem:test;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
        JdbcBackend.Database.forURL(url, driver = dbProfile.driver)
      case Postgresql | Mysql =>
        val props = new Properties()
        props.setProperty("dataSourceClassName", dbProfile.sourceClassName)
        props.setProperty("dataSource.user", dbConf.dbUsername)
        props.setProperty("dataSource.password", dbConf.dbPassword)
        props.setProperty("dataSource.databaseName", dbConf.dbName)
        props.setProperty("dataSource.serverName", dbConf.dbHost)
        props.setProperty("dataSource.portNumber", dbConf.dbPort)
        val hc = new HikariConfig(props)
        hc.setConnectionTestQuery("SELECT 1;")
        hc.setMaximumPoolSize(10)
        hc.setInitializationFailFast(true)
        try {
          val ds = new HikariDataSource(hc)
          JdbcBackend.Database.forDataSource(ds, None)
        } catch {
          case x: Exception =>
            Console.err.println(s"Database Initialization error. Check parameters for db: $x")
            sys.exit(1)
        }
    }

    val driver = CurrentDriver(dbProfile.profile)

    import driver.profile.api._

    val tables = Await.result(db.run(MTable.getTables), 10 seconds)
      .toList.map(_.name).map(_.name)

    if (!tables.contains("sources")) {
      Await.result(
        db.run {
          (driver.query.sources.schema ++ driver.query.feeds.schema).create
        },
        10 seconds
      )
    }

    DbLayer(db, driver)(ec)
  }
}

trait DBProfile {
  val isSqlite: Boolean = false
  val profile: JdbcProfile
  val driver: String
  val sourceClassName: String
}

object DBProfile {

  def get(x: String): Option[SupportedDb] = {
    x.toLowerCase match {
      case "h2" => Some(H2)
      case "postgresql" => Some(Postgresql)
      case "sqlite" => Some(Sqlite)
      case "mysql" => Some(Mysql)
      case _ => None
    }
  }

  def create(db: SupportedDb) = {
    db match {
      case H2 => new DBProfile {
        override val driver = "org.h2.Driver"
        override val profile: JdbcProfile = H2Profile
        override val sourceClassName = "org.h2.jdbcx.JdbcDataSource"
      }

      case Postgresql => new DBProfile {
        override val driver: String = "org.postgresql.Driver"
        override val profile: JdbcProfile = PostgresProfile
        override val sourceClassName: String = "org.postgresql.ds.PGSimpleDataSource"
      }

      case Sqlite => new DBProfile {
        override val profile: JdbcProfile = SQLiteProfile
        override val driver = "org.sqlite.JDBC"
        override val isSqlite: Boolean = true
        override val sourceClassName = ""
      }

      case Mysql => new DBProfile {
        override val driver: String = "com.mysql.jdbc.Driver"
        override val profile: JdbcProfile = MySQLProfile
        override val sourceClassName: String = "com.mysql.jdbc.jdbc2.optional.MysqlDataSource"
      }
    }
  }
}


