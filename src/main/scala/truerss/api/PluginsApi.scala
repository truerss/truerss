package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.dto.PluginsViewDto
import truerss.services.ApplicationPluginsService

import scala.concurrent.ExecutionContext

class PluginsApi(pluginsService: ApplicationPluginsService)(
  implicit val ec: ExecutionContext
) extends HttpHelper {

  import ApiImplicits._
  import JsonFormats._

  val route = api {
    pathPrefix("plugins") {
      get {
        pathPrefix("all")  {
          w[PluginsViewDto](pluginsService.view)
        } ~ pathPrefix("js") {
          // todo header js
          pluginsService.js
        } ~ pathPrefix("css") {
          pluginsService.css
        }
      }
    }
  }



}
