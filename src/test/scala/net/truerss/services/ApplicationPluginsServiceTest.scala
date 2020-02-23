package net.truerss.services

import java.net.{URI, URL}

import com.github.truerss.base._
import com.typesafe.config.Config
import net.truerss.Gen
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import truerss.services.ApplicationPluginsService
import truerss.util.ApplicationPlugins

import scala.collection.mutable.ArrayBuffer

class ApplicationPluginsServiceTest extends SpecificationLike with Mockito {

  private val fPrioriry = 10
  private val fName = "feed"
  private val sPrioriry = 11
  private val sName = "site"
  private val cPrioriry = 12
  private val cName = "content"

  "ApplicationPluginsService" should {
    "matchUrl" in {
      val testUrl1 = u(Gen.genUrl)
      val testUrl2 = u(Gen.genUrl)
      val testUrl3 = u(Gen.genUrl)
      val testUrl4 = u(Gen.genUrl)


      val ap = ApplicationPlugins(
        feedPlugins = ArrayBuffer(new TestFeedBasePlugin(testUrl1)),
        contentPlugins = ArrayBuffer(new TestBaseContentPlugin(testUrl2)),
        sitePlugins = ArrayBuffer(new TestSitePlugin(testUrl3))
      )

      val service = new ApplicationPluginsService(ap)

      service.matchUrl(testUrl1) must beTrue
      service.matchUrl(testUrl2) must beTrue
      service.matchUrl(testUrl3) must beTrue

      service.matchUrl(testUrl4) must beFalse
    }

    "getFeedReader by priority" in {
      val testUrl1 = u(Gen.genUrl)

      val f = new TestFeedBasePlugin(testUrl1)
      val s = new TestSitePlugin(testUrl1)
      val c = new TestBaseContentPlugin(testUrl1)

      val ap = ApplicationPlugins(
        feedPlugins = ArrayBuffer(f),
        contentPlugins = ArrayBuffer(c),
        sitePlugins = ArrayBuffer(s)
      )

      val service = new ApplicationPluginsService(ap)

      service.getFeedReader(testUrl1).get ==== s
    }

    "getContentReader by priority" in {
      val testUrl1 = u(Gen.genUrl)

      val f = new TestFeedBasePlugin(testUrl1)
      val s = new TestSitePlugin(testUrl1)
      val c = new TestBaseContentPlugin(testUrl1)

      val ap = ApplicationPlugins(
        feedPlugins = ArrayBuffer(f),
        contentPlugins = ArrayBuffer(c),
        sitePlugins = ArrayBuffer(s)
      )

      val service = new ApplicationPluginsService(ap)

      service.getContentReader(testUrl1).get ==== c
    }
  }

  private trait TestPluginInfo extends PluginInfo { self: BasePlugin =>
    override val author: String = "test"
    override val about: String = "test"
    override val pluginName: String = "test"
    override val version: String = "test"
  }

  private class TestFeedBasePlugin(expected: URL) extends BaseFeedPlugin(mock[Config]) with TestPluginInfo {
    override val priority: Int = fPrioriry

    override val pluginName: String = fName

    override def newEntries(url: String): Either[Errors.Error, Vector[Entry]] = Right(Vector.empty)

    override def matchUrl(url: URL): Boolean = url == expected
  }

  private class TestBaseContentPlugin(expected: URL) extends BaseContentPlugin(mock[Config]) with TestPluginInfo {
    override val priority: Int = cPrioriry

    override val pluginName: String = cName

    override val contentType: BaseType = Text

    override def matchUrl(url: URL): Boolean = expected == url

    override def content(urlOrContent: ContentTypeParam.RequestParam): Response = Right(Some("test"))
  }

  private class TestSitePlugin(expected: URL) extends BaseSitePlugin(mock[Config]) with TestPluginInfo {
    override val priority: Int = sPrioriry

    override val pluginName: String = sName

    override val contentType: BaseType = Text

    override def newEntries(url: String): Either[Errors.Error, Vector[Entry]] = Right(Vector.empty)

    override def matchUrl(url: URL): Boolean = expected == url

    override def content(urlOrContent: ContentTypeParam.RequestParam): Response = Right(Some("test"))
  }

  private def u(x: String): URL = new URL(x)

}
