package net.truerss.services

import net.truerss.Gen
import net.truerss.dao.FullDbHelper
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import truerss.db.validation.{SourceUrlValidator, SourceValidator}
import truerss.dto.{ApplicationPlugins, SourceDto, UpdateSourceDto}
import truerss.services.SourcesService
import truerss.services.management.FeedSourceDtoModelImplicits

import scala.concurrent.duration._

class SourceServiceTest(implicit ee: ExecutionEnv)
  extends FullDbHelper with SpecificationLike {

  import net.truerss.FutureTestExt._

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
      dao.insertMany(sx) ~> { _ => success }

      service.getAll ~> { xs =>
        xs.size ==== 2
        xs.map(_.name) must contain(allOf(sx.map(_.name).toSeq : _*))
      }

      service.getAllForOpml ~> {_.size ==== 2}
    }

    "get source by id" in {
      val source = Gen.genSource()
      val id = insert(source)
      service.getSource(id) ~> { _ must beSome(source.withId(id).toView) }
    }

    "mark as read" in {
      val source = Gen.genSource()
      val id = insert(source)
      val feed = Gen.genFeed(id, source.url).copy(read = false)

      dbLayer.feedDao.insert(feed) ~> { feedId =>
        service.markAsRead(id) ~> {_ must beSome }
        dbLayer.feedDao.findOne(feedId) ~> { x =>
          x must beSome
          x.get.read must beTrue
        }
      }
    }

    "delete source: should delete feeds and source" in {
      val source = Gen.genSource()
      val id = insert(source)
      val feed = Gen.genFeed(id, source.url)

      val feedId = a(dbLayer.feedDao.insert(feed))

      service.delete(id) ~> (_ must beSome)

      service.getSource(id) ~> (_ must beNone)
      dbLayer.feedDao.findBySource(feedId) ~> (_ must have size 0)
    }

    "add source" in {
      val source = Gen.genNewSource

      service.addSource(source) ~> (_ must beRight)
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
      service.updateSource(id, dto) ~> { x =>
        x must beRight

        val z = x.toOption.get

        z.name ==== newName
        z.url ==== url
        z.interval ==== interval

      }
    }
  }

}
