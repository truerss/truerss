package truerss.services

import truerss.db.DbLayer
import truerss.dto.{FeedDto, SearchRequest}
import truerss.services.management.FeedSourceDtoModelImplicits

import scala.concurrent.{ExecutionContext, Future}

class SearchService(dbLayer: DbLayer)(implicit val ec: ExecutionContext) {

  import FeedSourceDtoModelImplicits._

  private val dao = dbLayer.feedDao

  def search(request: SearchRequest): Future[Vector[FeedDto]] = {
    dao.search(request.inFavorites, request.query)
      .map { xs => xs.map(_.toDto).toVector}
  }

}
