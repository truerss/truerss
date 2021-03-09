package truerss.api

import truerss.dto.{FeedDto, Page, SearchRequest}
import truerss.services.SearchService
import com.github.fntz.omhs.{BodyReader, BodyWriter, ParamDSL}
import com.github.fntz.omhs.macros.RoutingImplicits
import com.github.fntz.omhs.playjson.JsonSupport

class SearchApi(private val searchService: SearchService) extends HttpApi {

  import JsonFormats._
  import RoutingImplicits._
  import ParamDSL._
  import ZIOSupport._

  private implicit val searchRequestReader: BodyReader[SearchRequest] =
    JsonSupport.reader[SearchRequest]
  private implicit val pageFeedDtoWriter: BodyWriter[Page[FeedDto]] =
    JsonSupport.writer[Page[FeedDto]]

  private val base = "api" / "v1"

  private val search = post(base / "search" / body[SearchRequest]) ~> { (q: SearchRequest) =>
    searchService.search(q)
  }

  val route = ???

}
