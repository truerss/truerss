package truerss.services

import truerss.db.DbLayer
import truerss.dto.{FeedDto, SearchRequest}
import truerss.services.FeedsService.{Page, toPage}
import truerss.services.SearchServiceR.SearchServiceR
import zio.{Has, Layer, Task, ZIO, ZLayer}

import scala.concurrent.ExecutionContext

object SearchServiceR {
  type SearchServiceR = Has[Service]

  trait Service {
    def search(request: SearchRequest): Task[Page]
  }

  final class LiveSearchService(private val dbLayer: DbLayer) extends Service {
    override def search(request: SearchRequest): Task[Page] = {
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

  def search(request: SearchRequest): ZIO[SearchServiceR, Throwable, Page] = {
    ZIO.accessM(_.get.search(request))
  }
}

class SearchService(dbLayer: DbLayer)(implicit val ec: ExecutionContext) {

  import FeedsService.{FPage, toPage}

  private val layer: Layer[Nothing, SearchServiceR] =
    ZLayer.succeed(new SearchServiceR.LiveSearchService(dbLayer))

  def search(request: SearchRequest): FPage = {
    val f: ZIO[SearchServiceR.SearchServiceR, Throwable, Page] = for {
      res <- SearchServiceR.search(request)
    } yield res

    f.provideLayer(layer)
  }

}
