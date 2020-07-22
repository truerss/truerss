package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.dto.PluginsViewDto
import truerss.services.ApplicationPluginsService

class PluginsApi(private val pluginsService: ApplicationPluginsService) extends HttpApi {

  import JsonFormats._

  val route = api {
    pathPrefix("plugins") {
      get {
        pathPrefix("all")  {
          w[PluginsViewDto](pluginsService.view)
        } ~ pathPrefix("js") {
          doneWith(pluginsService.js, HttpApi.javascript)
        } ~ pathPrefix("css") {
          doneWith(pluginsService.css, HttpApi.css)
        }
      }
    }
  }



}
