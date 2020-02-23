package net.truerss.dao

import java.io.File
import java.util.concurrent.Executors

import org.specs2.mutable.SpecificationLike
import org.specs2.specification.BeforeAfterAll
import slick.jdbc.JdbcBackend
import truerss.db.{DbLayer, Source}
import truerss.db.driver.{CurrentDriver, DBProfile, Sqlite, TableNames}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

trait FullDbHelper extends SpecificationLike with BeforeAfterAll {
  def dbName: String

  println(s"-------------> start db helper with db: $dbName")

  val callTime = 3 seconds
  val initTime = 3 seconds

  private lazy val dbProfile = DBProfile.create(Sqlite)

  private lazy val db = JdbcBackend.Database
    .forURL(s"jdbc:sqlite:./$dbName.tdb",
      driver = dbProfile.driver)

  private implicit lazy val driver = CurrentDriver(dbProfile.profile, TableNames.withPrefix(dbName))

  lazy val dbLayer: DbLayer = new DbLayer(db, driver)(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)))

  import driver.profile.api._

  val x = db.run {
    (driver.query.sources.schema ++ driver.query.feeds.schema).create
  }
  Await.result(x, initTime)

  override def beforeAll = {

  }

  override def afterAll = {
    db.run {
      (driver.query.sources.schema ++ driver.query.feeds.schema).drop
    }
    println(s"delete ===========> ${new File(s"./$dbName.tdb").delete()}")
  }

  def insert(x: Source): Long = {
    a(dbLayer.sourceDao.insert(x))
  }

  def a[T](x: Future[T]): T = {
    Await.result(x, callTime)
  }

  def sleep(x: Int) = {
    println(s"-------> wait: $x seconds")
    Thread.sleep(x * 1000)
  }

  def w = {
    sleep(3)
  }

}