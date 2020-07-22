package net.truerss.api

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTest, Specs2FrameworkInterface}
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.{AllExpectations, Scope}
import play.api.libs.json.{Json, Writes}

abstract class BaseApiTest extends RouteTest
  with SpecificationLike
  with AllExpectations
  with Specs2FrameworkInterface.Specs2 {
  protected val base = "/api/v1"
  protected def api(path: String) = s"$base/$path"

  protected def make[T: Writes](x: T): String = Json.toJson(x).toString
}


abstract class BaseScope extends Scope {

  protected val route: Route

}

