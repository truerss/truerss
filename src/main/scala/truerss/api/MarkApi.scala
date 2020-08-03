package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.services.MarkService

class MarkApi(private val markService: MarkService) extends HttpApi {

  import JsonFormats._

  val route = api {
    pathPrefix("mark") {
      put {
        pathEndOrSingleSlash {
          w[Unit](markService.markAll)
        } ~ pathPrefix(LongNumber) { sourceId =>
          w[Unit](markService.markOne(sourceId))
        }
      }
    }
  }

}
