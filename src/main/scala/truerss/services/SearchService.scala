package truerss.services

import truerss.db.DbLayer
import truerss.dto.{FeedDto, Page, SearchRequest}
import truerss.services.FeedsService.toPage
import zio.Task


class SearchService(private val dbLayer: DbLayer) {

  def search(request: SearchRequest): Task[Page[FeedDto]] = {
    dbLayer.feedDao
      .search(
        inFavorites = request.inFavorites,
        query = request.query,
        offset = request.offset,
        limit = request.limit
      ).map(toPage)
  }

}
