package truerss.db.driver

import java.nio.file.Paths
import java.time.{Clock, LocalDateTime}
import java.util.Properties

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import slick.jdbc._
import slick.jdbc.meta.MTable
import slick.migration.api._
import truerss.db._
import truerss.util.DbConfig

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

sealed trait SupportedDb
case object Postgresql extends SupportedDb
case object Sqlite extends SupportedDb
case object Mysql extends SupportedDb

object SupportedDb {

  private val waitTime = 10 seconds

  def load(dbConf: DbConfig, isUserConf: Boolean)(
          implicit ec: ExecutionContext
  ): DbLayer = {
    val backend: Option[SupportedDb] = DBProfile.get(dbConf.dbBackend)

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

    val names = TableNames.default
    val driver = CurrentDriver(dbProfile.profile, names)

    import driver.profile.api._

    val tables = Await.result(db.run(MTable.getTables), waitTime)

    val tableNames = tables.toList.map(_.name).map(_.name)

    if (!tableNames.contains(names.sources)) {
      Console.out.println("----> create db")
      Await.result(
        db.run {
          (driver.query.sources.schema ++ driver.query.feeds.schema).create
        },
        waitTime
      )
    }

    if (!tableNames.contains(names.predefinedSettings)) {
      Console.out.println("----> add predefined settings tables")
      Await.result(
        db.run {
          driver.query.predefinedSettings.schema.create
        },
        waitTime
      )
    }

    if (!tableNames.contains(names.userSettings)) {
      Console.out.println("----> add user settings tables")
      Await.result(
        db.run {
          driver.query.userSettings.schema.create
        },
        waitTime
      )
    }

    if (!tableNames.contains(names.versions)) {
      // no versions
      Await.result(db.run { driver.query.versions.schema.create }, waitTime)
    }

    runMigrations(db, dbProfile, driver)

    DbLayer(db, driver)(ec)
  }

  def runMigrations(db: JdbcBackend.DatabaseDef, dbProfile: DBProfile,
                    driver: CurrentDriver)(implicit ec: ExecutionContext) = {

    import driver.profile.api._

    val versions = Await.result(db.run(driver.query.versions.result), waitTime).toVector

    val currentSourceIndexes = driver.query.sources.baseTableRow.indexes.map(_.name)

    val v1 = Migration.addIndexes(dbProfile, driver, currentSourceIndexes)

    runPredefinedChanges(db, dbProfile, driver)

    val all = Vector(
      v1
    )
    val allVersions = versions.map(_.id)

    val need = all.filterNot { x => allVersions.contains(x.version) }

    Console.out.println(s"detect: ${versions.size} migrations, need to run: ${need.size}")

    need.foreach { m =>
      m.changes match {
        case Some(changes) =>
          Console.out.println(s"run: ${m.version} -> ${m.description}")
          Await.result(db.run(changes()), waitTime)
        case None =>
          Console.out.println(s"skip: ${m.version}: ${m.description}")
      }

      val version = Version(m.version, m.description, LocalDateTime.now(Clock.systemUTC()))
      val f = db.run {
        (driver.query.versions returning driver.query.versions.map(_.id)) += version
      }
      Await.result(f, waitTime)
    }

    Console.out.println("completed...")
  }

  def runPredefinedChanges(db: JdbcBackend.DatabaseDef, dbProfile: DBProfile,
                           driver: CurrentDriver)(implicit ec: ExecutionContext): Unit = {
    import driver.profile.api._
    Predefined.predefined.foreach { p =>
      val q = Await.result(
        db.run {
          driver.query.predefinedSettings.filter(_.key === p.key).result
        }, waitTime
      )
      if (q.isEmpty) {
        Console.println(s"Write: $p predefined settings")
        Await.ready(
          db.run {
            driver.query.predefinedSettings ++= p :: Nil
          }, waitTime
        )
      }
    }
  }

  case class Migration(version: Long, description: String, changes: Option[ReversibleMigrationSeq])

  object Migration {
    def addIndexes(dbProfile: DBProfile, driver: CurrentDriver, currentIndexes: Iterable[String]): Migration = {
      implicit val dialect = GenericDialect.apply(dbProfile.profile)

      val changes = if (currentIndexes.isEmpty) {
        val sq = TableMigration(driver.query.sources)
          .addIndexes(_.byUrlIndex)
          .addIndexes(_.byNameIndex)

        val fq = TableMigration(driver.query.feeds)
          .addIndexes(_.bySourceIndex)
          .addIndexes(_.byFavoriteIndex)
          .addIndexes(_.byReadIndex)
          .addIndexes(_.bySourceAndFavorite)
          .addIndexes(_.byTitleIndex)
          .addIndexes(_.bySourceAndReadIndex)
        Some(sq & fq)
      } else {
        None
      }



      Migration(1L, "add indexes", changes)
    }
  }


}

trait DBProfile {
  val isSqlite: Boolean = false
  val profile: JdbcProfile
  val driver: String
  val sourceClassName: String
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
        override val isSqlite: Boolean = true
        override val sourceClassName: String = ""
      }

      case Mysql => new DBProfile {
        override val driver: String = "com.mysql.jdbc.Driver"
        override val profile: JdbcProfile = MySQLProfile
        override val sourceClassName: String = "com.mysql.jdbc.jdbc2.optional.MysqlDataSource"
      }
    }
  }
}


