package net.truerss.services

import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import org.specs2.mock.Mockito
import org.specs2.specification.{BeforeAfterEach, Scope}
import truerss.dto.Setup
import truerss.services.{ApplicationPluginsService, ContentReaderService, FeedsService, SettingsService}
import truerss.util.syntax

class ContentReaderServiceTest(implicit ee: ExecutionEnv)
  extends SpecificationLike with Mockito with BeforeAfterEach {

  sequential

  import net.truerss.FutureTestExt._
  import syntax.future._
  import syntax.ext._

  private val feedId = 1L
  private val content = "test"
  private val dto = Gen.genFeedDto.copy(content = None)



  "content reader" should {
    "just return if content is present" in new MyTest(content.some.right, false) {
      val tmp = dto.copy(content = content.some)
      service.readFeedContent(feedId, tmp, forceReadContent = false) ~> { res =>
        res.error must beNone

        res.feedDto ==== tmp
      }
      org.mockito.Mockito.verifyNoInteractions(feedsServiceM)
      org.mockito.Mockito.verifyNoInteractions(settingsM)
    }

    "process if setup enabled" in new MyTest(content.some.right, false) {
      val tmp = dto.copy(content = None)
      service.readFeedContent(feedId, tmp, forceReadContent = false) ~> { res =>
        res.error must beNone

        res.feedDto ==== tmp.copy(content = content.some)
      }

      org.mockito.Mockito.verify(settingsM, org.mockito.Mockito.times(1)).where(
        ContentReaderService.readContentKey, ContentReaderService.defaultIsRead
      )
      org.mockito.Mockito.verify(feedsServiceM, org.mockito.Mockito.times(1)).updateContent(
        feedId, content
      )
    }

    "do not process if setup disabled" in new MyTest(content.some.right, true) {
      val tmp = dto.copy(content = None)
      service.readFeedContent(feedId, tmp, forceReadContent = false) ~> { res =>
        res.error must beNone

        res.feedDto ==== tmp
      }

      org.mockito.Mockito.verify(settingsM, org.mockito.Mockito.times(1)).where(
        ContentReaderService.readContentKey, ContentReaderService.defaultIsRead
      )
      org.mockito.Mockito.verifyNoInteractions(feedsServiceM)
    }

    "force process" in new MyTest(content.some.right, false) {
      val tmp = dto.copy(content = None)
      service.readFeedContent(feedId, tmp, forceReadContent = true) ~> { res =>
        res.error must beNone

        res.feedDto ==== tmp.copy(content = content.some)
      }

      org.mockito.Mockito.verifyNoInteractions(settingsM)
      org.mockito.Mockito.verify(feedsServiceM, org.mockito.Mockito.times(1)).updateContent(
        feedId, content
      )
    }
  }

  override protected def before: Any = {}

  override protected def after: Any = {
    org.mockito.Mockito.validateMockitoUsage()
  }

  private class MyTest(readResult: Either[String, Option[String]], skipContent: Boolean) extends Scope {
    val feedsServiceM = mock[FeedsService].verbose
    val appPluginsM = mock[ApplicationPluginsService].verbose
    val settingsM = mock[SettingsService].verbose

    val service = new ContentReaderService(feedsServiceM, appPluginsM, settingsM)(
      scala.concurrent.ExecutionContext.Implicits.global
    ) {
      override protected def read(url: String): Either[String, Option[String]] = {
        readResult
      }
    }

    feedsServiceM.updateContent(feedId, content) returns ().toF

    val setup = Setup[Boolean](ContentReaderService.readContentKey, skipContent).toF
    settingsM.where(ContentReaderService.readContentKey, ContentReaderService.defaultIsRead) returns setup

  }

}
