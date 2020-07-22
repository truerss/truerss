package net.truerss.api
import java.io.IOException

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import net.truerss.Gen
import play.api.libs.json.Json
import truerss.api.SearchApi
import truerss.dto.{FeedDto, Page, SearchRequest}
import truerss.services.SearchService
import truerss.api.JsonFormats
import zio.Task

class SearchApiTests extends BaseApiTest {

  import JsonFormats._

  private val searchRequest = SearchRequest(inFavorites = false, query = "test")
  private val dtos = Gen.genFeedDto :: Nil
  private val page = Page[FeedDto](dtos.size, dtos)

  "search api" should {
    "process search" in new Test(Task.succeed(page)) {
      Post(api("search"), Json.toJson(searchRequest).toString()) ~> route ~> check {
        responseAs[String] === Json.toJson(page).toString
        status ==== StatusCodes.OK
      }
    }

    "process when something went wrong with db" in new Test(Task.fail(new IOException("boom"))) {
      Post(api("search"), Json.toJson(searchRequest).toString()) ~> route ~> check {
        status ==== StatusCodes.InternalServerError
      }
    }

    "random text" in new Test(Task.succeed(page)) {
      Post(api("search"), "asd") ~> route ~> check {
        responseAs[String] must contain("Unable to parse request:")
        status ==== StatusCodes.BadRequest
      }
    }

    "invalid json" in new Test(Task.succeed(page)) {
      Post(api("search"), Json.toJson(page).toString()) ~> route ~> check {
        responseAs[String] must contain("Unable to parse request:")
        status ==== StatusCodes.BadRequest
      }
    }
  }

  private class Test(result: Task[Page[FeedDto]]) extends BaseScope {
    val searchService = new SearchService(null) {
      override def search(request: SearchRequest): Task[Page[FeedDto]] = {
        result
      }
    }
    override protected val route: Route = new SearchApi(searchService).route
  }

}
