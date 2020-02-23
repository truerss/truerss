package net.truerss.services

import akka.event.EventStream
import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import truerss.api.SourceResponse
import truerss.services.{OpmlService, SourcesService}
import truerss.services.management.SourcesManagement
import truerss.services.management.FeedSourceDtoModelImplicits
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.util.Util.ResponseHelpers

import scala.concurrent.Future
import scala.concurrent.duration._

class SourcesManagementTest(implicit val ee: ExecutionEnv) extends Specification with Mockito {

  sequential

  import FeedSourceDtoModelImplicits._

  private val sourceId = 1L
  private val dto = Gen.genSource(Some(sourceId)).toView
  private val newSource = Gen.genNewSource
  private val updateSource = Gen.genUpdSource(sourceId)
  private val sm = mock[SourcesService]
  private val om = mock[OpmlService]
  private val es = mock[EventStream]
  private val s = new SourcesManagement(sm, om, es)
  sm.delete(sourceId) returns f(Some(dto))
  sm.addSource(newSource) returns f(Right(dto))
  sm.updateSource(sourceId, updateSource) returns f(Right(dto))

  "sources management" should {
    "delete source" in {
      s.deleteSource(sourceId) must be_==(ResponseHelpers.ok).await

      there was one(sm).delete(sourceId)
      there was one(es).publish(SourcesKeeperActor.SourceDeleted(dto))
    }

    "add source" in {
      s.addSource(newSource) must be_==(SourceResponse(dto)).await

      there was one(sm).addSource(newSource)
      there was one(es).publish(SourcesKeeperActor.NewSource(dto))
    }

    "update source" in {
      s.updateSource(sourceId, updateSource) must be_==(SourceResponse(dto)).await

      there was one(sm).updateSource(sourceId, updateSource)
      there was one(es).publish(SourcesKeeperActor.ReloadSource(dto))
    }

    "force all" in {
      s.forceRefresh must be_==(ResponseHelpers.ok).await

      there was one(es).publish(SourcesKeeperActor.Update)
      there was no(sm)
    }

    "force single" in {
      s.forceRefreshSource(sourceId) must be_==(ResponseHelpers.ok).await

      there was after(10.millis).one(es).publish(SourcesKeeperActor.UpdateOne(sourceId))
      there was no(sm)
    }
  }

  private def f[T](x: T) = Future.successful(x)

}
