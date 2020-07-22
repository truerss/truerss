package net.truerss.api

import akka.event.EventStream
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import truerss.api.RefreshApi
import truerss.services.RefreshSourcesService
import zio.Task

class RefreshApiTests extends BaseApiTest {

  private val path = "refresh"
  private val sourceId_200 = 1

  "refresh api" should {
    "single" in new Test() {
      Put(api(s"$path/$sourceId_200")) ~> route ~> check {
        status ==== StatusCodes.NoContent
      }
    }

    "all" in new Test() {
      Put(api(s"$path")) ~> route ~> check {
        status ==== StatusCodes.NoContent
      }
    }
  }

  private class Test() extends BaseScope {
    val service = new RefreshSourcesService(null) {
      override def refreshSource(sourceId: Long): Task[Unit] = {
        Task.succeed(())
      }

      override def refreshAll: Task[Unit] = {
        Task.succeed(())
      }
    }
    override protected val route: Route = new RefreshApi(service).route
  }

}
