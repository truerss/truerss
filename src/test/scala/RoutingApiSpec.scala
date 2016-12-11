
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.{RouteTest, TestFrameworkInterface}
import akka.testkit.TestProbe
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.AllExpectations
import truerss.api.RoutingApiImpl


class RoutingApiSpec extends RouteTest
  with SpecificationLike
  with AllExpectations
  with TestFrameworkInterface {
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

  def checkRoute(x: HttpRequest) = {
    x ~> route ~> check {
      status ==== StatusCodes.OK
    }
    _ok
  }
  def r(x: HttpRequest) = checkRoute(x)

  section("api", "http")
  "source api" should {
    "get all" in {
      r(Get(s"$sourcesUrl/all"))
    }

    "get source by id" in {
      r(Get(s"$sourcesUrl/123"))
    }

    "create source" in {
      r(Post(s"$sourcesUrl"))
    }

    "delete source" in {
      r(Delete(s"$sourcesUrl/123"))
    }

    "update source" in {
      r(Put(s"$sourcesUrl/123"))
    }

    "mark all" in {
      r(Put(s"$sourcesUrl/markAll"))
    }

    "mark source as read" in {
      r(Put(s"$sourcesUrl/mark/123"))
    }

    "get unread feeds" in {
      r(Get(s"$sourcesUrl/unread/123"))
    }

    "get latest feeds for all sources" in {
      r(Get(s"$sourcesUrl/latest/100"))
    }

    "get feeds by source" in {
      r(Get(s"$sourcesUrl/feeds/123"))
    }

    "refresh all" in {
      r(Put(s"$sourcesUrl/refresh"))
    }

    "refresh one source" in {
      r(Put(s"$sourcesUrl/refresh/123"))
    }

    "import new sources" in {
      r(Post(s"$sourcesUrl/import"))
    }

    "get opml" in {
      r(Get(s"$sourcesUrl/opml"))
    }

  }

  section("http", "api")
  "feeds api" should {
    "get favorites" in {
      r(Get(s"$feedsUrl/favorites"))
    }
    "get one feed" in {
      r(Get(s"$feedsUrl/123"))
    }
    "mark as favorite" in {
      r(Put(s"$feedsUrl/mark/123"))
    }
    "remove from favorite list" in {
      r(Put(s"$feedsUrl/unmark/123"))
    }
    "mark as read" in {
      r(Put(s"$feedsUrl/read/123"))
    }
    "mark as unread" in {
      r(Put(s"$feedsUrl/unread/123"))
    }
  }

  section("http", "api")
  "plugins api" should {
    "get all" in {
      r(Get(s"$pluginsUrl/all"))
    }
    "get js" in {
      r(Get(s"$pluginsUrl/js"))
    }
    "get css" in {
      r(Get(s"$pluginsUrl/css"))
    }
  }

  section("http", "api")
  "system api" should {
    "stop" in {
      r(Get(s"$systemUrl/stop"))
    }
    "restart" in {
      r(Get(s"$systemUrl/restart"))
    }
    "exit" in {
      r(Get(s"$systemUrl/exit"))
    }
  }




}
