package net.truerss.services

import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import truerss.db.validation.{SourceUrlValidator, SourceValidator}
import truerss.dto.{SourceDto, UpdateSourceDto}
import truerss.services.SourcesService
import truerss.services.management.FeedSourceDtoModelImplicits
import truerss.util.ApplicationPlugins

import scala.concurrent.duration._

class SourceServiceTest(implicit ee: ExecutionEnv)
  extends FullDbHelper with SpecificationLike {

  override def dbName: String = "source_service_test"

  import FeedSourceDtoModelImplicits._

  sequential

  val dao = dbLayer.sourceDao

  val plugins = ApplicationPlugins()

  val service = new SourcesService(dbLayer, plugins) {
    protected override val sourceValidator = new SourceValidator(plugins)(dbLayer, ee.executionContext) {
      protected override val sourceUrlValidator = new SourceUrlValidator() {
        override def validateUrl(dto: SourceDto): Either[String, SourceDto] = {
          Right(dto)
        }
      }
    }
  }

  "SourceService" should {
    "getAll && opml" in {
      val source1 = Gen.genSource()
      val source2 = Gen.genSource()
      val sx = Iterable(source1, source2)
      dao.insertMany(sx)

      val result = service.getAll
      result.map { xs =>
        xs.size ==== 2
        xs.map(_.name) must contain(allOf(sx.map(_.name).toSeq : _*))
      }.await

      service.getAllForOpml.map(_.size ==== 2).await
    }

    "get source by id" in {
      val source = Gen.genSource()
      val id = insert(source)
      println(s"-------------------------------> ${id}")
      val result = a(service.getSource(id))
      result must beSome(source.withId(id).toView)
    }

    "mark as read" in {
      val source = Gen.genSource()
      val id = insert(source)
      val feed = Gen.genFeed(id, source.url)

      feed.read must beFalse

      val feedId = a(dbLayer.feedDao.insert(feed))

      service.markAsRead(id).map(_ must beSome).await

      w

      dbLayer.feedDao.findOne(feedId).map { x =>
        x must beSome
        x.get.read must beTrue
      }.await
    }

    "delete source: should delete feeds and source" in {
      val source = Gen.genSource()
      val id = insert(source)
      val feed = Gen.genFeed(id, source.url)

      val feedId = a(dbLayer.feedDao.insert(feed))

      service.delete(id).map(_ must beSome).await(3, 3 seconds)

      service.getSource(id).map(_ must beNone).await
      dbLayer.feedDao.findBySource(feedId).map(_ must have size 0).await
    }

    "add source" in {
      val source = Gen.genNewSource

      service.addSource(source).map(_ must beRight).await
    }

    "update source" in {
      val source = Gen.genSource()
      val id = insert(source)
      val newName = s"${source.name}------>new name"
      val url = Gen.genUrl
      val interval = source.interval + 10
      val dto = UpdateSourceDto(
        id = id,
        url = url,
        name = newName,
        interval = interval
      )
      service.updateSource(id, dto).map { x =>
        x must beRight

        val z = x.toOption.get

        z.name ==== newName
        z.url ==== url
        z.interval ==== interval

      }.await
    }
  }

}
