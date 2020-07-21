package net.truerss.services.management

import akka.event.EventStream
import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import truerss.api.SourceResponse
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.services.{OpmlService, SourceOverviewService, SourcesService}
import truerss.util.syntax

import scala.concurrent.duration._

class SourcesManagementTest(implicit val ee: ExecutionEnv) extends Specification with Mockito {

  sequential

  import truerss.util.FeedSourceDtoModelImplicits._
  import syntax.future._

  private val sourceId = 1L
  private val dto = Gen.genSource(Some(sourceId)).toView
  private val newSource = Gen.genNewSource
  private val updateSource = Gen.genUpdSource(sourceId)
  private val sm = mock[SourcesService]
  private val om = mock[OpmlService]
  private val som = mock[SourceOverviewService]
  private val es = mock[EventStream]
  private val s = new SourcesManagement(sm, om, som, es)
  sm.delete(sourceId) returns Some(dto).toF
  sm.addSource(newSource) returns Right(dto).toF
  sm.updateSource(sourceId, updateSource) returns Right(dto).toF

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
}
