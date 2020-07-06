package net.truerss.api

import akka.http.scaladsl.model.{HttpRequest, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTest, Specs2FrameworkInterface}
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.AllExpectations
import play.api.libs.json.{Json, Writes}

import scala.concurrent.Future

abstract class BaseApiTest extends RouteTest
  with SpecificationLike
  with AllExpectations
  with Specs2FrameworkInterface.Specs2
  with Mockito {

  protected val r: Route

  protected val nf = StatusCodes.NotFound

  protected def checkR[W: Writes](req: HttpRequest, res: W,
                                resultStatus: StatusCode = StatusCodes.OK) = {
    req ~> r ~> check {
      if (resultStatus == StatusCodes.OK) {
        responseAs[String] ==== Json.toJson(res).toString
      }
      if (status != resultStatus) {
        println(s"failed with ${responseAs[String]}")
      }
      status ==== resultStatus
    }
  }

  protected def checkR(req: HttpRequest, resultStatus: StatusCode) = {
    req ~> r ~> check {
      if (status != resultStatus) {
        println(s"failed with ${responseAs[String]}")
      }
      status ==== resultStatus
    }
  }
}
