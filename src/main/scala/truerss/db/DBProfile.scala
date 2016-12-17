package truerss.db

import slick.jdbc.JdbcProfile
import slick.jdbc.SQLiteProfile
import slick.jdbc.PostgresProfile
import slick.jdbc.H2Profile
import slick.jdbc.MySQLProfile

sealed trait SupportedDb
case object H2 extends SupportedDb
case object Postgresql extends SupportedDb
case object Sqlite extends SupportedDb
case object Mysql extends SupportedDb

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


