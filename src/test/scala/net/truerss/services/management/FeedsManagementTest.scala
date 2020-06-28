package net.truerss.services.management

import akka.event.EventStream
import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import truerss.api.{FeedResponse, InternalServerErrorResponse}
import truerss.dto.Setup
import truerss.services.management.{FeedsManagement, ResponseHelpers}
import truerss.services.{ContentReaderService, FeedsService, PublishPluginActor, SettingsService}

import scala.concurrent.Future

class FeedsManagementTest(implicit val ee: ExecutionEnv) extends Specification with Mockito {

  private val feedId = 1L
  private val feedId1 = 10L
  private val feedId2 = 100L
  private val feedId3 = 1000L
  private val content = "test"
  private val boom = "boom"
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

      "read content only if content-setting is enabled" in {
        val feedsServiceM = mock[FeedsService]
        val readerServiceM = mock[ContentReaderService]
        val settingsM = mock[SettingsService]
        val streamM = mock[EventStream]
        feedsServiceM.findOne(feedId) returns f(Some(dto.copy(content = None)))
        settingsM.where[Boolean](FeedsManagement.readContentKey,
          FeedsManagement.defaultIsRead) returns f(Setup[Boolean](FeedsManagement.readContentKey, true))

        val service = new FeedsManagement(feedsServiceM, readerServiceM, settingsM, streamM)

        service.getFeed(feedId) must be_==(FeedResponse(dto.copy(content = None))).await

        there was one(settingsM).where(FeedsManagement.readContentKey,
          FeedsManagement.defaultIsRead)
        there was no(readerServiceM)
      }

      "return 500 when content-reader service is not available" in new MyTest() {
        feedM.getFeed(feedId2) must be_==(InternalServerErrorResponse(boom)).await

        there was one(feedsServiceM).findOne(feedId2)
        there was one(readerServiceM).read(noContentDto.url)
      }

      "return 200 if content was read" in new MyTest() {
        feedM.getFeed(feedId3) must be_==(FeedResponse(noContentDto1.copy(content = Some(content)))).await

        there was one(feedsServiceM).findOne(feedId3)
        there was one(readerServiceM).read(noContentDto1.url)
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
    val settingsM = mock[SettingsService]
    val streamM = mock[EventStream]
    val feedM = new FeedsManagement(feedsServiceM, readerServiceM, settingsM, streamM)
    feedsServiceM.changeFav(feedId, favFlag = true) returns f(Some(dto))
    feedsServiceM.changeFav(feedId, favFlag = false) returns f(Some(dto))
    feedsServiceM.findOne(feedId) returns f(Some(dto))

    feedsServiceM.findOne(feedId1) returns f(None)
    feedsServiceM.findOne(feedId2) returns f(Some(noContentDto))
    feedsServiceM.findOne(feedId3) returns f(Some(noContentDto1))

    readerServiceM.read(noContentDto.url) returns Left(boom)
    readerServiceM.read(noContentDto1.url) returns Right(Some(content))
    val setup = Future.successful(Setup[Boolean](FeedsManagement.readContentKey, false))
    settingsM.where(FeedsManagement.readContentKey, FeedsManagement.defaultIsRead) returns setup
  }

  def f[T](x: T) = Future.successful(x)

}
