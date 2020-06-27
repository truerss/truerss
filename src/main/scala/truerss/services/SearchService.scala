package truerss.services

import truerss.db.DbLayer
import truerss.dto.SearchRequest

import scala.concurrent.ExecutionContext

class SearchService(dbLayer: DbLayer)(implicit val ec: ExecutionContext) {

  import FeedsService.{FPage, toPage}

  private val dao = dbLayer.feedDao


  def search(request: SearchRequest): FPage = {
    dao.search(request.inFavorites, request.query, request.offset, request.limit).map(toPage)
  }

}
