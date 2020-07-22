package net.truerss.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import play.api.libs.json.Json
import truerss.api.{JsonFormats, SourcesOverviewApi}
import truerss.db.DbLayer
import truerss.dto.SourceOverview
import truerss.services.{NotFoundError, SourceOverviewService}
import zio.Task

class SourcesOverviewApiTests extends BaseApiTest {

  import JsonFormats._

  private val path = "overview"
  private val sourceId = 1L
  private val overview = SourceOverview.empty(sourceId)

  "overview api" should {
    "#ok" in new Test(Task.succeed(overview)) {
      Get(api(path)) ~> route ~> check {
        responseAs[String] ==== Json.toJson(overview).toString
        status ==== StatusCodes.OK
      }
    }

    "#notFound" in new Test(Task.fail(NotFoundError(sourceId))) {
      Get(api(path)) ~> route ~> check {
        status ==== StatusCodes.NotFound
      }
    }
  }

  private class Test(result: Task[SourceOverview]) extends BaseScope {
    val service = new SourceOverviewService(null) {
      override def getSourceOverview(sourceId: Long): Task[SourceOverview] = result
    }
    override protected val route: Route = new SourcesOverviewApi(service).route
  }

}
