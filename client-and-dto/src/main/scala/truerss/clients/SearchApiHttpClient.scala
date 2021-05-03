package truerss.clients

import play.api.libs.json.Json
import truerss.dto.{FeedDto, Page, SearchRequest}
import zio.Task

class SearchApiHttpClient(baseUrl: String) extends BaseHttpClient(baseUrl) {

  import JsonSupport._

  protected val searchUrl = s"$baseUrl/$api/search"

  def search(request: SearchRequest): Task[Page[FeedDto]] = {
    post[Page[FeedDto]](searchUrl, Json.toJson(request).toString())
  }

  def search(query: String): Task[Page[FeedDto]] = {
    search(SearchRequest(
      inFavorites = false,
      query = query,
      offset = 0,
      limit = 100
    ))
  }

}
