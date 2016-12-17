
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.{RouteTest, TestFrameworkInterface}
import akka.testkit.TestProbe
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.AllExpectations
import truerss.api.RoutingApiImpl
import truerss.models.ApiJsonProtocol
import truerss.system.{db, plugins, util, global}

import scala.reflect.ClassTag
import scala.concurrent.duration._

class RoutingApiSpec extends RouteTest
  with SpecificationLike
  with AllExpectations
  with TestFrameworkInterface {

  import db._
  import util._
  import plugins._
  import global._
  import spray.json._
  import ApiJsonProtocol._

  override def failTest(msg: String): Nothing = {
    println(s"test failed $msg")
    ???
  }

  val service = TestProbe()

  val httpApi = new RoutingApiImpl(service.ref)
  val route = httpApi.route

  val _ok = Unit ==== Unit

  val baseUrl = "/api/v1"
  val sourcesUrl = s"$baseUrl/sources"
  val feedsUrl = s"$baseUrl/feeds"
  val pluginsUrl = s"$baseUrl/plugins"
  val systemUrl = s"$baseUrl/system"

  def checkRoute(x: HttpRequest, msg: Any) = {
    x ~> route
    service.expectMsg(10 seconds, msg)

    _ok
  }

  def checkRoute1[T: ClassTag](x: HttpRequest) = {
    x ~> route
    service.expectMsgType[T](10 seconds)

    _ok
  }

  def c[T: ClassTag](x: HttpRequest) = {
    x ~> route ~> check {
      status ==== StatusCodes.OK
    }
    _ok
  }

  def r(x: HttpRequest, msg: Any) = checkRoute(x, msg)
  def r1[T: ClassTag](x: HttpRequest) = checkRoute1(x)


  section("api", "http")
  "source api" should {
    "get all" in {
      r(Get(s"$sourcesUrl/all"), GetAll)
    }

    "get source by id" in {
      r(Get(s"$sourcesUrl/123"), GetSource(123L))
    }

    "create source" in {
      val s = Gen.genSource(None)
      r1[AddSource](Post(s"$sourcesUrl", s.toJson.toString()))
    }

    "delete source" in {
      r(Delete(s"$sourcesUrl/123"), DeleteSource(123))
    }

    "update source" in {
      val s = Gen.genSource(None)
      r1[UpdateSource](Put(s"$sourcesUrl/123", s.toJson.toString()))
    }

    "mark all" in {
      r(Put(s"$sourcesUrl/markall"), MarkAll)
    }

    "mark source as read" in {
      r(Put(s"$sourcesUrl/mark/123"), Mark(123))
    }

    "get unread feeds" in {
      r(Get(s"$sourcesUrl/unread/123"), Unread(123))
    }

    "get latest feeds for all sources" in {
      r(Get(s"$sourcesUrl/latest/100"), Latest(100))
    }

    "get feeds by source" in {
      r(Get(s"$sourcesUrl/feeds/123"), ExtractFeedsForSource(123, 0, 100))
      r(Get(s"$sourcesUrl/feeds/123?from=10"), ExtractFeedsForSource(123, 10, 100))
      r(Get(s"$sourcesUrl/feeds/123?limit=400"), ExtractFeedsForSource(123, 0, 400))
      r(Get(s"$sourcesUrl/feeds/123?limit=asd"), ExtractFeedsForSource(123, 0, 100))
    }

    "refresh all" in {
      r(Put(s"$sourcesUrl/refresh"), Update)
    }

    "refresh one source" in {
      r(Put(s"$sourcesUrl/refresh/123"), UpdateOne(123))
    }

    "import new sources" in {
      c(Post(s"$sourcesUrl/import"))
    }

    "get opml" in {
      r(Get(s"$sourcesUrl/opml"), Opml)
    }

  }

  section("http", "api")
  "feeds api" should {
    "get favorites" in {
      r(Get(s"$feedsUrl/favorites"), Favorites)
    }
    "get one feed" in {
      r(Get(s"$feedsUrl/123"), GetFeed(123))
    }
    "mark as favorite" in {
      r(Put(s"$feedsUrl/mark/123"), MarkFeed(123))
    }
    "remove from favorite list" in {
      r(Put(s"$feedsUrl/unmark/123"), UnmarkFeed(123))
    }
    "mark as read" in {
      r(Put(s"$feedsUrl/read/123"), MarkAsReadFeed(123))
    }
    "mark as unread" in {
      r(Put(s"$feedsUrl/unread/123"), MarkAsUnreadFeed(123))
    }
  }

  section("http", "api")
  "plugins api" should {
    "get all" in {
      r(Get(s"$pluginsUrl/all"), GetPluginList)
    }
    "get js" in {
      r(Get(s"$pluginsUrl/js"), GetJs)
    }
    "get css" in {
      r(Get(s"$pluginsUrl/css"), GetCss)
    }
  }

  section("http", "api")
  "system api" should {
    "stop" in {
      r(Get(s"$systemUrl/stop"), StopSystem)
    }
    "restart" in {
      r(Get(s"$systemUrl/restart"), RestartSystem)
    }
    "exit" in {
      r(Get(s"$systemUrl/exit"), StopApp)
    }
  }




}
