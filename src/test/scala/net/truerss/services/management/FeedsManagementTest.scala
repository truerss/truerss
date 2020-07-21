package net.truerss.services.management

import akka.event.EventStream
import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import truerss.api.FeedResponse
import truerss.services.actors.events.PublishPluginActor
import truerss.services.management.FeedsManagement
import truerss.services.{ContentReaderService, FeedsService}
import truerss.util.syntax

class FeedsManagementTest(implicit val ee: ExecutionEnv) extends Specification with Mockito {

  import syntax.future._

  private val feedId = 1L
  private val feedId1 = 10L
  private val feedId2 = 100L
  private val feedId3 = 1000L
  private val dto = Gen.genFeedDto.copy(id = feedId, content = Some("test"))
  private val noContentDto = Gen.genFeedDto.copy(content = None)
  private val noContentDto1 = Gen.genFeedDto.copy(content = None)


  "feeds management" should {
    "publish message on favorites" in new MyTest() {
      feedM.changeFavorites(feedId, favFlag = true)

      there was one(feedsServiceM).changeFav(feedId, favFlag = true)
      there was one(streamM).publish(PublishPluginActor.PublishEvent(dto))
    }

    "do not publish when change to Unmark" in new MyTest() {
      feedM.changeFavorites(feedId, favFlag = false)
      there was one(feedsServiceM).changeFav(feedId, favFlag = false)
      there was no(streamM).publish(PublishPluginActor.PublishEvent(dto))
    }

    "read one" should {
      "just return if content is present" in new MyTest() {
        feedM.getFeed(feedId) must be_==(FeedResponse(dto)).await

        there was one(feedsServiceM).findOne(feedId)
        there was no(readerServiceM)
      }

      "404 if feed was not found" in new MyTest() {
        feedM.getFeed(feedId1) must be_==(ResponseHelpers.feedNotFound).await


        there was one(feedsServiceM).findOne(feedId1)
        there was no(readerServiceM)
      }
    }

  }

  private class MyTest extends Scope {
    val feedsServiceM = mock[FeedsService]
    val readerServiceM = mock[ContentReaderService]
    val streamM = mock[EventStream]
    val feedM = new FeedsManagement(feedsServiceM, readerServiceM, streamM)
    feedsServiceM.changeFav(feedId, favFlag = true) returns Some(dto).toF
    feedsServiceM.changeFav(feedId, favFlag = false) returns Some(dto).toF
    feedsServiceM.findOne(feedId) returns Some(dto).toF

    feedsServiceM.findOne(feedId1) returns None.toF
    feedsServiceM.findOne(feedId2) returns Some(noContentDto).toF
    feedsServiceM.findOne(feedId3) returns Some(noContentDto1).toF
  }
}
