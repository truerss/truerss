package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.services.management.PluginsManagement

import scala.concurrent.ExecutionContext

class PluginsApi(pluginsService: PluginsManagement)(
  implicit val ec: ExecutionContext
) extends HttpHelper {

  import ApiImplicits._

  val route = api {
    pathPrefix("plugins") {
      get {
        pathPrefix("all")  {
          pluginsService.getPluginList
        } ~ pathPrefix("js") {
          pluginsService.getJs
        } ~ pathPrefix("css") {
          pluginsService.getCss
        }
      }
    }
  }



}
