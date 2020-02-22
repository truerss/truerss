package net.truerss.services

import akka.http.scaladsl.model.StatusCodes
import truerss.api._
import truerss.dto.PluginsViewDto
import truerss.services.management.PluginsManagement

class PluginsApiTest extends BaseApiTest {

  import JsonFormats._

  private val dto = PluginsViewDto()
  private val pm = mock[PluginsManagement]
  pm.getJs returns JsResponse("js")
  pm.getCss returns CssResponse("css")
  pm.getPluginList returns AppPluginsResponse(dto)

  protected override val r = new PluginsApi(pm).route

  "api" should {
    "get css" in {
      Get("/api/v1/plugins/css") ~> r ~> check {
        status ==== StatusCodes.OK
      }
      there was one(pm).getCss
    }

    "get js" in {
      Get("/api/v1/plugins/js") ~> r ~> check {
        status ==== StatusCodes.OK
      }
      there was one(pm).getJs
    }

    "get plugins" in {
      checkR(Get("/api/v1/plugins/all"), dto)
      there was one(pm).getPluginList
    }
  }

}
