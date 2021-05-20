package net.truerss.tests

import net.truerss.{Gen, Resources, ZIOMaterializer}
import org.specs2.mutable.Specification
import truerss.dto.SourceStatusDto
import truerss.services.{NotFoundError, SourceStatusesService}

trait SourceStatusesServiceTests extends Specification with Resources {

  import ZIOMaterializer._

  private lazy val service = new SourceStatusesService(dbLayer)

  "source statuses service" should {
    "increment error, when row is not exist" in {
      // create new source
      val source = Gen.genSource(None).copy(name = "source statuses service")
      val sourceId = dbLayer.sourceDao.insert(source).m
      // increment
      service.incrementError(sourceId).m
      // check
      service.findOne(sourceId).m ==== SourceStatusDto(sourceId, 1)
      // increment again
      service.incrementError(sourceId).m
      // and again
      service.incrementError(sourceId).m
      service.findOne(sourceId).m ==== SourceStatusDto(sourceId, 3)
      service.findAll.m.filter(_.sourceId == sourceId) ==== Iterable(SourceStatusDto(sourceId, 3))
      // delete source
      dbLayer.sourceDao.delete(sourceId).m
      dbLayer.sourceStatusesDao.delete(sourceId).m
      service.findOne(sourceId).e must beLeft(NotFoundError(sourceId))
      success
    }
  }


}
