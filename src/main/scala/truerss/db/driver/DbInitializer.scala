package truerss.db.driver

import java.time.{Clock, LocalDateTime}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import slick.ast.FieldSymbol
import slick.dbio.DBIO
import slick.jdbc.{JdbcBackend, JdbcProfile}
import slick.jdbc.meta.{MColumn, MQName, MTable}
import slick.migration.api.{Dialect, GenericDialect, ReversibleMigrationSeq, TableMigration}
import truerss.db.{DbLayer, PluginSource, Predefined, Version}
import truerss.util.DbConfig

import scala.concurrent.Await
import scala.concurrent.duration._

object DbInitializer {

  private val waitTime = 10 seconds
  private val initFailTimeout = 1000

  def initialize(dbConf: DbConfig, isUserConf: Boolean): DbLayer = {
    val backend: Option[SupportedDb] = DBProfile.get(dbConf.dbBackend)

    if (backend.isEmpty) {
      Console.err.println(s"Unsupported database backend: ${dbConf.dbBackend}")
      sys.exit(1)
    }

    val dbProfile = DBProfile.create(backend.get)

    val props = dbProfile.props(dbConf, isUserConf)
    val hc = new HikariConfig(props)
    hc.setConnectionTestQuery("SELECT 1;")
    hc.setPoolName("TrueRssPool")
    hc.setInitializationFailTimeout(initFailTimeout)
    hc.setMaximumPoolSize(dbProfile.defaultConnectionSize)

    val db = try {
      val ds = new HikariDataSource(hc)
      JdbcBackend.Database.forDataSource(ds, None)
    } catch {
      case ex: Exception =>
        Console.err.println(s"Database Initialization error. Check parameters for the db: $ex")
        sys.exit(1)
    }

    val names = TableNames.default
    val driver = CurrentDriver(dbProfile.profile, names)

    createTables(db, driver)

    runMigrations(db, dbProfile, driver)

    DbLayer(db, driver)
  }

  private def createTables(db: JdbcBackend.DatabaseDef, driver: CurrentDriver): Unit = {
    import driver.profile.api._

    def run[T](description: String, act: DBIOAction[T, NoStream, Nothing]) = {
      Console.out.println(s"----> $description")
      Await.result(
        db.run {
          act
        },
        waitTime
      )
    }

    val names = driver.tableNames

    val tables = Await.result(db.run(MTable.getTables), waitTime)

    val tableNames = tables.toList.map(_.name).map(_.name)

    if (!tableNames.contains(names.sources)) {
      run("create db", (driver.query.sources.schema ++ driver.query.feeds.schema).create)
    }

    if (!tableNames.contains(names.predefinedSettings)) {
      run("add predefined settings tables", driver.query.predefinedSettings.schema.create)
    }

    if (!tableNames.contains(names.userSettings)) {
      run("add user settings tables", driver.query.userSettings.schema.create)
    }

    if (!tableNames.contains(names.versions)) {
      // no versions
      run("create versions table", driver.query.versions.schema.create)
    }

    if (!tableNames.contains(names.pluginSources)) {
      // no plugin sources
      run("create plugin_sources table", driver.query.pluginSources.schema.create)
    }

    if (!tableNames.contains(names.sourceStatuses)) {
      run(s"create ${names.sourceStatuses} table", driver.query.sourceStatuses.schema.create)
    }
  }

  private def runMigrations(db: JdbcBackend.DatabaseDef, dbProfile: DBProfile, driver: CurrentDriver): Unit = {

    import driver.profile.api._

    val versions = Await.result(db.run(driver.query.versions.result), waitTime).toVector

    val currentSourceIndexes = driver.query.sources.baseTableRow.indexes.map(_.name)

    val v1 = Migration.addIndexes(dbProfile, driver, currentSourceIndexes)

    val v2 = Migration.addEnclosure(db, dbProfile, driver)

    runPredefinedChanges(db, driver)

    addDefaultSource(db, driver)

    val all = Vector(
      v1,
      v2
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
  // todo remove
  def addDefaultSource(db: JdbcBackend.DatabaseDef, driver: CurrentDriver): Unit = {
    import driver.profile.api._
    val url = "https://github.com/truerss/plugins/releases/tag/1.0.0"
    val length = Await.result(
      db.run {
        driver.query.pluginSources.filter(_.url === url).length.result
      }, waitTime
    )
    if (length == 0) {
      Console.println(s"Write: default plugin source")
      Await.ready(
        db.run {
          driver.query.pluginSources ++= PluginSource(id = None, url = url) :: Nil
        }, waitTime
      )
    }

  }

  def runPredefinedChanges(db: JdbcBackend.DatabaseDef, driver: CurrentDriver): Unit = {
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
          .addIndexes(_.bySourceAndReadIndex)
        Some(sq & fq)
      } else {
        None
      }

      Migration(1L, "add indexes", changes)
    }

    def addEnclosure(
      db: JdbcBackend.DatabaseDef,
      dbProfile: DBProfile,
      driver: CurrentDriver
    ): Migration = {
      val feedsColumnsQuery = MColumn.getColumns(
        MQName.local(driver.query.feeds.baseTableRow.tableName),
        "enclosure"
      )
      val columns = Await.result(db.run(feedsColumnsQuery), waitTime)
      implicit val dialect: Dialect[_ <: JdbcProfile] = GenericDialect(dbProfile.profile)
      val changes = Option.when(columns.isEmpty)(new ReversibleMigrationSeq(
        TableMigration(driver.query.feeds).addColumns(_.enclosure)
      ))
      Migration(2L, "add enclosure", changes)
    }
  }
}
