package net.truerss.api

import net.truerss.Gen
import play.api.libs.json._
import truerss.api.{FeedsResponse, JsonFormats, SearchApi}
import truerss.dto.SearchRequest
import truerss.util.syntax
import truerss.services.management.FeedSourceDtoModelImplicits

import scala.concurrent.Future

class SearchApiTest extends BaseApiTest {

  import FeedSourceDtoModelImplicits._
  import JsonFormats._
  import syntax.future._

  private val sm = mock[SearchManagement]
  private val request = SearchRequest(inFavorites = false, query = "test", offset = 0, limit = 10)
  private val feed = Gen.genFeed(1, "dsa").toDto
  sm.search(request) returns FeedsResponse(Vector(feed)).toF

  protected override val r = new SearchApi(sm).route

  private val url = "/api/v1/search"

  "api" should {
    "process request" in {
      val j = Json.toJson(request).toString()
      checkR(Post(url, j), Vector(feed))
      there was one(sm).search(request)
    }
  }

}
