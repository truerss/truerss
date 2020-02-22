package truerss.api

import akka.actor.ActorRef
import akka.stream.Materializer
import akka.http.scaladsl.server.Directives._
import truerss.services.management.PluginsManagement

import scala.concurrent.ExecutionContext

class PluginsApi(pluginsService: PluginsManagement)(
  implicit override val ec: ExecutionContext,
  val materializer: Materializer
) extends HttpHelper {


  override val service: ActorRef = null

  val route = api {
    pathPrefix("plugins") {
      get {
        pathPrefix("all") {
          call(pluginsService.getPluginList)
        } ~ pathPrefix("js") {
          call(pluginsService.getJs)
        } ~ pathPrefix("css") {
          call(pluginsService.getCss)
        }
      }
    }
  }



}
