package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.dto.SearchRequest
import truerss.services.management.SearchManagement

import scala.concurrent.ExecutionContext

class SearchApi(val searchManagement: SearchManagement)(
  implicit val ec: ExecutionContext
) extends HttpHelper {

  import JsonFormats._

  val route = api {
    pathPrefix("search") {
      post {
        createT[SearchRequest](searchManagement.search)
      }
    }
  }

}
