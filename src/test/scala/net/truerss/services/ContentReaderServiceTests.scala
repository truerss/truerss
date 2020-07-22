package net.truerss.services

import org.specs2.mutable.Specification
import net.truerss.{Gen, ZIOMaterializer}
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import truerss.dto.{FeedContent, Setup}
import truerss.services.{ContentReaderService, FeedsService, NotFoundError, ReaderClient, SettingsService}
import zio.Task

class ContentReaderServiceTests extends Specification with Mockito {

  import ZIOMaterializer._
  import ContentReaderService._

  private val feedId = 1L
  private val url = "http://example.com/rss"
  private val content = "test"
  private val dto = Gen.genFeedDto.copy(id = feedId, content = None, url = url)

  "content reader service" should {
    "#ok" in {
      success
    }

    "feed was not found" in new Test() {
      feedsService.findOne(feedId) returns Task.fail(NotFoundError(feedId))
      init()

      service.fetchFeedContent(feedId).e must beLeft(NotFoundError(feedId))

      there was one(feedsService).findOne(feedId)
    }

    "do not read when content is present" in {
      success
    }

    "read only when setting is enabled" in new Test() {
      feedsService.findOne(feedId) returns Task.succeed(dto)
      settingsService.where[Boolean](readContentKey, defaultIsRead) returns Task.succeed(
        Setup(readContentKey, false)
      )
      readerClient.read(url) returns Task.succeed(Some(content))
      feedsService.updateContent(feedId, content) returns Task.succeed(())

      init()

      service.fetchFeedContent(feedId).e must beRight(FeedContent(Some(content)))

      there was one(feedsService).findOne(feedId)
      there was one(settingsService).where[Boolean](readContentKey, defaultIsRead)
      there was one(readerClient).read(url)
      there was one(feedsService).updateContent(feedId, content)
    }

    "do not update content in db when content is given" in new Test() {
      val withContent = dto.copy(content = Some(content))
      feedsService.findOne(feedId) returns Task.succeed(withContent)

      init()

      service.fetchFeedContent(feedId).m ==== FeedContent(Some(content))

      there was one(feedsService).findOne(feedId)
      there was no(settingsService)
    }

    "do not read when setting is disabled" in new Test() {
      feedsService.findOne(feedId) returns Task.succeed(dto)
      settingsService.where[Boolean](readContentKey, defaultIsRead) returns Task.succeed(
        Setup(readContentKey, true)
      )

      init()

      service.fetchFeedContent(feedId).m ==== FeedContent(None)

      there was one(feedsService).findOne(feedId)
      there was one(settingsService).where[Boolean](readContentKey, defaultIsRead)
      there was no(readerClient)
    }
  }

  private class Test extends Scope {
    val feedsService = mock[FeedsService]
    val readerClient = mock[ReaderClient]
    val settingsService = mock[SettingsService]

    var service: ContentReaderService = _

    def init() = {
      service = new ContentReaderService(feedsService, readerClient, settingsService)
    }
  }

}
