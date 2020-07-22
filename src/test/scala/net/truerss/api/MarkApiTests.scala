package net.truerss.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import truerss.api.MarkApi
import truerss.db.DbLayer
import truerss.services.MarkService
import zio.Task

class MarkApiTests extends BaseApiTest {

  private val path = "mark"
  private val sourceId = 1L

  "mark api" should {
    "mark one" in new Test() {
      Put(api(s"$path/$sourceId")) ~> route ~> check {
        status ==== StatusCodes.NoContent
      }
    }

    "mark all" in new Test() {
      Put(api(s"$path")) ~> route ~> check {
        status ==== StatusCodes.NoContent
      }
    }
  }

  private class Test() extends BaseScope {
    val service = new MarkService(null) {
      override def markAll: Task[Unit] = Task.succeed(())

      override def markOne(sourceId: Long): Task[Unit] = Task.succeed(())
    }
    override protected val route: Route = new MarkApi(service).route
  }

}
