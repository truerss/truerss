package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.services.RefreshSourcesService

class RefreshApi(private val refreshSourcesService: RefreshSourcesService) extends HttpApi {

  import JsonFormats._

  val route = api {
    pathPrefix("refresh") {
      pathEndOrSingleSlash {
        w[Unit](refreshSourcesService.refreshAll)
      } ~
      pathPrefix(LongNumber) { sourceId =>
        w[Unit](refreshSourcesService.refreshSource(sourceId))
      }
    }
  }

}
