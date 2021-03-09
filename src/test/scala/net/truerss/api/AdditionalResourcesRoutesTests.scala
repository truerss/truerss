package net.truerss.api

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTest, Specs2FrameworkInterface}
import org.specs2.mutable.SpecificationLike
import truerss.api.AdditionalResourcesRoutes

class AdditionalResourcesRoutesTests
  extends RouteTest with SpecificationLike with Specs2FrameworkInterface.Specs2 {

  import AdditionalResourcesRoutes._

  private val wsPort = 10000
  private val route = new AdditionalResourcesRoutes(wsPort).route
  private val cookie = "Set-Cookie".toLowerCase()
  private val r = redirectToLocation
  private val o = portC

  "routes" should {
    "handle requests" in {
      Get("/about") ~> addHeader(xhrHeaderName, xhrHeaderValue) ~> Route.seal(route) ~> check {
        cookies must be empty
      }
      Get("/about") ~> Route.seal(route) ~> check {
        cookies must have size 2
        first.value() ==== make(o, s"$wsPort")
        last.value() ==== make(r, "/about")
      }
      Get("/show/asd/1") ~> Route.seal(route) ~> check {
        cookies must have size 2
        first.value() ==== make(o, s"$wsPort")
        last.value() ==== make(r, "/show/asd/1")
      }
    }
  }

  private def cookies = response.headers.filter(_.is(cookie))
  private def first = cookies.head
  private def last = cookies.last

  private def make(a: String, b: String): String = {
    s"$a=$b"
  }

}
