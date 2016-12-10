
import java.util.Date

import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterAll
import org.specs2.concurrent.ExecutionEnv
import truerss.models.{Enable, Source, SourceDao, SourceState}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Await

class SourceDaoSpec(implicit ee: ExecutionEnv) extends Specification with DbHelper with BeforeAfterAll {

  sequential

  implicit val duration = 3 seconds

  override val dbName = "sourceDaoSpec"

  import driver.profile.api._

  override def beforeAll = {
    db.run {
      (driver.query.sources.schema ++ driver.query.feeds.schema).create
    }
  }

  override def afterAll = {
    db.run {
      (driver.query.sources.schema ++ driver.query.feeds.schema).drop
    }
  }

  val sourceDao = new SourceDao(db)

  section("dao")
  "source dao" should {
    "return all sources in db" in {
      sourceDao.all must empty.await

      sourceDao.insert(Gen.genSource()) must be_>(0L).await

      sourceDao.all must not be empty.await
    }

    "find one source" in {
      val source = Gen.genSource()
      val id = insert(source)

      sourceDao.findOne(id) must beSome(source.copy(id = Some(id))).await

      sourceDao.findOne(10000L) must beNone.await
    }

    "delete source" in {
      val id = insert()

      sourceDao.delete(id) must be_==(1).await

      sourceDao.findOne(id) must beNone.await
    }

    "update source" in {
      Unit ==== Unit
    }

    "update last date" in {
      val id = insert()
      val date = new Date()

      sourceDao.updateLastUpdateDate(id, date) must be_==(1).await

      sourceDao.findOne(id).map(s => s.map(_.lastUpdate)) must beSome(date).await

      // when source missed
      sourceDao.updateLastUpdateDate(10000L, date) must be_==(0).await
    }

    "update state" in {
      val id = insert()
      val state: SourceState = Enable

      sourceDao.updateState(id, state) must be_==(1).await

      sourceDao.findOne(id).map(s => s.map(_.state)) must beSome(state).await
    }


  }

  def insert(source: Source = Gen.genSource()): Long = {
    Await.result(sourceDao.insert(source), duration)
  }

}
