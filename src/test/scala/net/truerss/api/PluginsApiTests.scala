package net.truerss.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import org.specs2.mock.Mockito
import truerss.api.{HttpApi, PluginsApi}
import truerss.dto.{ApplicationPlugins, PluginsViewDto}
import truerss.services.ApplicationPluginsService
import zio._

class PluginsApiTests extends BaseApiTest {

  private val path = "plugins"
  private val rjs = "<script></script>"
  private val rCss = "<style></style>"
  private val rView = PluginsViewDto()
  private val pluginsServiceM = new ApplicationPluginsService(ApplicationPlugins()) {
    override def js: UIO[String] = UIO.succeed(rjs)

    override def css: UIO[String] = UIO.succeed(rCss)

    override def view: UIO[PluginsViewDto] = UIO.succeed(rView)
  }

  private val route: Route = new PluginsApi(pluginsServiceM).route

  "plugins api" should {
    "get plugins" in {
      Get(api(s"$path/all")) ~> route ~> check {
        status ==== StatusCodes.OK
      }
    }

    "get js" in {
      Get(api(s"$path/js")) ~> route ~> check {
        contentType ==== HttpApi.javascript
        status ==== StatusCodes.OK
      }
    }

    "get css" in {
      Get(api(s"$path/css")) ~> route ~> check {
        contentType ==== HttpApi.css
        status ==== StatusCodes.OK
      }
    }
  }


}
