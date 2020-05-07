package net.truerss.services.management

import akka.event.EventStream
import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import truerss.api.{FeedResponse, InternalServerErrorResponse}
import truerss.db.Predefined
import truerss.dto.Setup
import truerss.services.management.FeedsManagement
import truerss.services.{ContentReaderService, FeedsService, PublishPluginActor, SettingsService}
import truerss.util.Util.ResponseHelpers

import scala.concurrent.Future

class FeedsManagementTest(implicit val ee: ExecutionEnv) extends Specification with Mockito {

  sequential

  private val feedId = 1L
  private val feedId1 = 10L
  private val feedId2 = 100L
  private val feedId3 = 1000L
  private val content = "test"
  private val boom = "boom"
  private val dto = Gen.genFeedDto.copy(id = feedId, content = Some("test"))
  private val noContentDto = Gen.genFeedDto.copy(content = None)
  private val noContentDto1 = Gen.genFeedDto.copy(content = None)
  private val fs = mock[FeedsService]
  private val cr = mock[ContentReaderService]
  private val sm = mock[SettingsService]
  private val es = mock[EventStream]
  private val f = new FeedsManagement(fs, cr, sm, es)
  fs.addToFavorites(feedId) returns f(Some(dto))
  fs.findOne(feedId) returns f(Some(dto))
  fs.findOne(feedId1) returns f(None)
  fs.findOne(feedId2) returns f(Some(noContentDto))
  fs.findOne(feedId3) returns f(Some(noContentDto1))

  cr.read(noContentDto.url) returns Left(boom)
  cr.read(noContentDto1.url) returns Right(Some(content))
  val key = Predefined.read.toKey
  val setup = Future.successful(Setup[Boolean](key, true))
  sm.where(key, true) returns setup


  "feeds management" should {
    "publish message on favorites" in {
      f.addToFavorites(feedId)

      there was one(fs).addToFavorites(feedId)
      there was one(es).publish(PublishPluginActor.PublishEvent(dto))
    }

    "read one" should {
      "just return if content is present" in {
        f.getFeed(feedId) must be_==(FeedResponse(dto)).await

        there was one(fs).findOne(feedId)
        there was no(cr)
      }

      "read content only if content-setting is enabled" in {
        val fs = mock[FeedsService]
        val cr = mock[ContentReaderService]
        val sm = mock[SettingsService]
        val es = mock[EventStream]
        fs.findOne(feedId) returns f(Some(dto.copy(content = None)))
        sm.where[Boolean](Predefined.read.toKey, true) returns Future.successful(Setup[Boolean](Predefined.read.toKey, false))
        val service = new FeedsManagement(fs, cr, sm, es)

        service.getFeed(feedId) must be_==(FeedResponse(dto.copy(content = None))).await

        there was one(sm).where(key, true)
        there was no(cr)
      }

      "return 500 when content-reader service is not available" in {
        f.getFeed(feedId2) must be_==(InternalServerErrorResponse(boom)).await

        there was one(fs).findOne(feedId2)
        there was one(cr).read(noContentDto.url)
      }

      "return 200 if content was read" in {
        f.getFeed(feedId3) must be_==(FeedResponse(noContentDto1.copy(content = Some(content)))).await

        there was one(fs).findOne(feedId3)
        there was one(cr).read(noContentDto1.url)
      }

      "404 if feed was not found" in {
        f.getFeed(feedId1) must be_==(ResponseHelpers.feedNotFound).await

        there was one(fs)
        there was no(cr)
      }
    }

  }

  def f[T](x: T) = Future.successful(x)

}
