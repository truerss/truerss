package truerss.services.management

import truerss.api.FeedsResponse
import truerss.dto.SearchRequest
import truerss.services.SearchService

import scala.concurrent.ExecutionContext

class SearchManagement(val searchService: SearchService)
                      (implicit val ec: ExecutionContext) extends BaseManagement {

  def search(request: SearchRequest): R = {
    searchService.search(request).map(FeedsResponse)
  }

}
