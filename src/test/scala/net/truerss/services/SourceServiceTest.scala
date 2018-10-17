package net.truerss.services

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import truerss.dto.UpdateSourceDto
import truerss.services.SourcesService
import truerss.services.actors.DtoModelImplicits
import truerss.util.ApplicationPlugins

class SourceServiceTest(implicit ee: ExecutionEnv)
  extends FullDbHelper("source_service_test") with SpecificationLike {

  import DtoModelImplicits._

  sequential

  val dao = dbLayer.sourceDao

  val plugins = ApplicationPlugins()

  val service = new SourcesService(dbLayer, plugins)

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

      service.delete(id).map(_ must beSome).await

      service.getSource(id).map(_ must beNone).await
      dbLayer.feedDao.findBySource(id).map(_ must have size 0).await
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
