
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

  val httpApi = new RoutingApiImpl()
  val route = httpApi.route

  val _ok = Unit ==== Unit

  val url = "/api/v1/sources"

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
      r(Get(s"$url/all"))
    }

    "get source by id" in {
      r(Get(s"$url/123"))
    }

    "create source" in {
      r(Post(s"$url"))
    }

    "delete source" in {
      r(Delete(s"$url/123"))
    }

    "update source" in {
      r(Put(s"$url/123"))
    }

    "mark all" in {
      r(Put(s"$url/markAll"))
    }

    "mark source as read" in {
      r(Put(s"$url/mark/123"))
    }

    "get unread feeds" in {
      r(Get(s"$url/unread/123"))
    }

    "get latest feeds for all sources" in {
      r(Get(s"$url/latest/100"))
    }

    "get feeds by source" in {
      r(Get(s"$url/feeds/123"))
    }

    "refresh all" in {
      r(Put(s"$url/refresh"))
    }

    "refresh one source" in {
      r(Put(s"$url/refresh/123"))
    }

    "import new sources" in {
      r(Post(s"$url/import"))
    }

    "get opml" in {
      r(Get(s"$url/opml"))
    }

  }

}
