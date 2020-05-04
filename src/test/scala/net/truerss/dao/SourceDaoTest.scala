package net.truerss.dao

import java.time.{LocalDateTime, ZoneId, ZoneOffset}
import java.util.Date

import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.BeforeAfterAll
import truerss.db.{SourceState, SourceStates}
import truerss.services.SourceOverviewService

import scala.concurrent.duration._

class SourceDaoTest(implicit ee: ExecutionEnv) extends FullDbHelper
  with SpecificationLike with BeforeAfterAll {

  import SourceOverviewService._

  override def dbName = "source_dao_spec"

  sequential

  implicit val duration = 3 seconds

  val sourceDao = dbLayer.sourceDao

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
      val id = a(sourceDao.insert(Gen.genSource()))
      println(s"===> delete id:$id")
      sourceDao.delete(id) must be_==(1).await

      sourceDao.findOne(id) must beNone.await
    }

    "update source" in {
      val source = Gen.genSource()
      val id = insert(source)
      val newName = "new-source-name"
      val newUrl = "http://new-url.com"

      val updSource = source.copy(
        id = Some(id),
        name = newName,
        url = newUrl
      )

      sourceDao.updateSource(updSource) must be_==(1).await

      sourceDao.findOne(id) must beSome(updSource).await
    }

    "update last date" in {
      val id = a(sourceDao.insert(Gen.genSource()))
      val date = LocalDateTime.now(ZoneOffset.UTC)

      sourceDao.updateLastUpdateDate(id, date) must be_==(1).await

      sourceDao.findOne(id).map(s => s.map(_.lastUpdate)) must beSome(date).await

      // when source missed
      sourceDao.updateLastUpdateDate(10000L, date) must be_==(0).await
    }

    "update state" in {
      val id = a(sourceDao.insert(Gen.genSource()))
      val state: SourceState = SourceStates.Enable

      sourceDao.updateState(id, state) must be_==(1).await

      sourceDao.findOne(id).map(s => s.map(_.state)) must beSome(state).await
    }

    "find source by url" in {
      val source = Gen.genSource()
      val id = insert(source)

      val source1 = source
      val id1 = insert(source1)

      // when `id` is present
      sourceDao.findByUrl(source.url, Some(id)) must be_==(1).await
      // when `id` is missed
      sourceDao.findByUrl(source.url, None) must be_==(2).await
    }

    "find by name" in {
      val source = Gen.genSource()
      val id = insert(source)

      val source1 = source
      val id1 = insert(source1)

      // when `id` is present
      sourceDao.findByName(source.name, Some(id)) must be_==(1).await
      // when `id` is missed
      sourceDao.findByName(source.name, None) must be_==(2).await
    }

  }

}
