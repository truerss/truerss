package truerss.db

import scala.slick.driver.JdbcDriver
import scala.slick.driver.JdbcDriver._
import scala.slick.driver.SQLiteDriver
import scala.slick.driver.PostgresDriver
import scala.slick.driver.H2Driver

sealed trait SupportedDb
case object H2 extends SupportedDb
case object Postgresql extends SupportedDb
case object Sqlite extends SupportedDb

trait DBProfile {
  val isSqlite: Boolean = false
  val profile: JdbcDriver
  val driver: String
  val sourceClassName: String
}

object DBProfile {

  def get(x: String): Option[SupportedDb] = {
    x.toLowerCase match {
      case "h2" => Some(H2)
      case "postgresql" => Some(Postgresql)
      case "sqlite" => Some(Sqlite)
      case _ => None
    }
  }

  def create(db: SupportedDb) = {
    db match {
      case H2 => new DBProfile {
        override val driver = "org.h2.Driver"
        override val profile: JdbcDriver = H2Driver
        override val sourceClassName = "org.h2.jdbcx.JdbcDataSource"
      }

      case Postgresql => new DBProfile {
        override val driver: String = "org.postgresql.Driver"
        override val profile: JdbcDriver = PostgresDriver
        override val sourceClassName: String = "org.postgresql.ds.PGSimpleDataSource"
      }

      case Sqlite => new DBProfile {
        override val profile: JdbcDriver = SQLiteDriver
        override val driver = "org.sqlite.JDBC"
        override val isSqlite: Boolean = true
        override val sourceClassName = ""
      }
    }
  }
}


