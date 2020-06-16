package net.truerss.dao

import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.BeforeAfterAll
import truerss.db.{SourceState, SourceStates}

import scala.concurrent.duration._

class SourceDaoTest(implicit ee: ExecutionEnv) extends FullDbHelper
  with SpecificationLike with BeforeAfterAll {

  import net.truerss.FutureTestExt._

  override def dbName = "source_dao_spec"

  sequential

  implicit val duration = 3 seconds

  val sourceDao = dbLayer.sourceDao

  "source dao" should {
    "return all sources in db" in {
      sourceDao.all ~> { _ must empty }

      sourceDao.insert(Gen.genSource()) ~> { _ must be_>(0L) }

      sourceDao.all ~> { _ must not be empty }
    }

    "find one source" in {
      val source = Gen.genSource()
      val id = insert(source)

      sourceDao.findOne(id) ~> { x =>
        x must beSome

        val result = x.get

        result.url ==== source.url
        result.name ==== source.name
        result.state ==== source.state
      }

      sourceDao.findOne(10000L) ~> { _ must beNone }
    }

    "delete source" in {
      val id = a(sourceDao.insert(Gen.genSource()))
      sourceDao.delete(id) ~> { _ must be_==(1) }

      sourceDao.findOne(id) ~> { _ must beNone }
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

      sourceDao.updateSource(updSource) ~> { _ must be_==(1) }

      sourceDao.findOne(id) ~> { _ must beSome(updSource) }
    }

    "update last date" in {
      val id = a(sourceDao.insert(Gen.genSource()))
      val date = Gen.now

      sourceDao.updateLastUpdateDate(id, date) ~> { _ must be_==(1) }

      sourceDao.findOne(id).map(s => s.map(_.lastUpdate)) ~> { _ must beSome(date) }

      // when source missed
      sourceDao.updateLastUpdateDate(10000L, date) ~> { _ must be_==(0) }
    }

    "update state" in {
      val id = a(sourceDao.insert(Gen.genSource()))
      val state: SourceState = SourceStates.Enable

      sourceDao.updateState(id, state) ~> { _ must be_==(1) }

      sourceDao.findOne(id).map(s => s.map(_.state)) ~> { _ must beSome(state) }
    }

    "find source by url" in {
      val source = Gen.genSource()
      val id = insert(source)

      val source1 = source
      val _ = insert(source1)

      // when `id` is present
      sourceDao.findByUrl(source.url, Some(id)) ~> { _ must be_==(1) }
      // when `id` is missed
      sourceDao.findByUrl(source.url, None) ~> { _ must be_==(2) }
    }

    "find by name" in {
      val source = Gen.genSource()
      val id = insert(source)

      val source1 = source
      val _ = insert(source1)

      // when `id` is present
      sourceDao.findByName(source.name, Some(id)) ~> { _ must be_==(1) }
      // when `id` is missed
      sourceDao.findByName(source.name, None) ~> { _ must be_==(2) }
    }

  }

}
