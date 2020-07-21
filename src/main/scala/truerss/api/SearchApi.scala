package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.dto.{FeedDto, Page, SearchRequest}
import truerss.services.SearchService

class SearchApi(private val searchService: SearchService) extends HttpApi {

  import JsonFormats._

  val route = api {
    pathPrefix("search") {
      post {
        createTR[SearchRequest, Page[FeedDto]](searchService.search)
      }
    }
  }

}
