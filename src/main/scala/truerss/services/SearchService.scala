package truerss.services

import truerss.db.DbLayer
import truerss.dto.SearchRequest
import zio.Task

import scala.concurrent.ExecutionContext

class SearchService(dbLayer: DbLayer)(implicit val ec: ExecutionContext) {

  import FeedsService.{FPage, toPage}

  def search(request: SearchRequest): FPage = {
    Task.fromFuture { implicit ec =>
      dbLayer.feedDao
        .search(
          inFavorites = request.inFavorites,
          query = request.query,
          offset = request.offset,
          limit = request.limit
        ).map(toPage)
    }
  }

}
