package net.truerss.services

import java.util.concurrent.Executors

import org.specs2.mutable.SpecificationLike
import org.specs2.specification.BeforeAfterAll
import slick.jdbc.JdbcBackend
import truerss.db.DbLayer
import truerss.db.drivers.{CurrentDriver, DBProfile, H2}
import truerss.models.Source

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

trait DbHelper {

  val dbName: String = "test1"

  val dbProfile = DBProfile.create(H2)
  val db = JdbcBackend.Database
    .forURL(s"jdbc:h2:mem:$dbName;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
      driver = dbProfile.driver)
  implicit val driver = CurrentDriver(dbProfile.profile)

}

class FullDbHelper(dbName: String) extends SpecificationLike with BeforeAfterAll {
  require(dbName.trim.nonEmpty)

  println(s"-------------> start db helper with db: $dbName")

  val callTime = 3 seconds
  val initTime = 10 seconds

  private lazy val dbProfile = DBProfile.create(H2)
  private lazy val db = JdbcBackend.Database
    .forURL(s"jdbc:h2:mem:$dbName;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
      driver = dbProfile.driver)
  private implicit lazy val driver = CurrentDriver(dbProfile.profile)

  lazy val dbLayer: DbLayer = new DbLayer(db, driver)(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1)))

  import driver.profile.api._

  override def beforeAll = {
    val x = db.run {
      (driver.query.sources.schema ++ driver.query.feeds.schema).create
    }
    Await.result(x, initTime)
  }

  override def afterAll = {
    db.run {
      (driver.query.sources.schema ++ driver.query.feeds.schema).drop
    }
  }

  def insert(x: Source): Long = {
    a(dbLayer.sourceDao.insert(x))
  }

  def a[T](x: Future[T]): T = {
    Await.result(x, callTime)
  }

}