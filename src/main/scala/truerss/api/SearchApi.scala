package truerss.api

import akka.stream.Materializer
import akka.http.scaladsl.server.Directives._
import truerss.dto.SearchRequest
import truerss.services.management.SearchManagement

import scala.concurrent.ExecutionContext

class SearchApi(val searchManagement: SearchManagement)(
  implicit override val ec: ExecutionContext,
  val materializer: Materializer
) extends HttpHelper {

  import JsonFormats._

  val route = api {
    pathPrefix("search") {
      post {
        create[SearchRequest](x => searchManagement.search(x))
      }
    }
  }

}
