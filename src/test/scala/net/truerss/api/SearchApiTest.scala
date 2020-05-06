package net.truerss.api

import net.truerss.Gen
import play.api.libs.json._
import truerss.api.{FeedsResponse, JsonFormats, SearchApi}
import truerss.dto.SearchRequest
import truerss.services.management.{FeedSourceDtoModelImplicits, SearchManagement}

import scala.concurrent.Future

class SearchApiTest extends BaseApiTest {

  import FeedSourceDtoModelImplicits._
  import JsonFormats._

  private val sm = mock[SearchManagement]
  private val request = SearchRequest(inFavorites = false, query = "test")
  private val feed = Gen.genFeed(1, "dsa").toDto
  sm.search(request) returns Future.successful(FeedsResponse(Vector(feed)))

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
