package truerss.api

import com.github.fntz.omhs.RoutingDSL
import truerss.dto.SearchRequest
import truerss.services.SearchService

class SearchApi(private val searchService: SearchService) {

  import OMHSSupport._
  import RoutingDSL._
  import ZIOSupport._

  val route = post("api" / "v1" / "search" <<< body[SearchRequest]) ~> { (q: SearchRequest) =>
    searchService.search(q)
  }

}
