package net.truerss.api
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import truerss.api.{HttpApi, OpmlApi}
import truerss.dto.SourceViewDto
import truerss.services.{OpmlService, SourcesService}
import zio.Task

class OpmlApiTests extends BaseApiTest {

  private val path = "opml"

  private val opmlContent = "<test></test>"

  "opml api" should {
    "download current opml" in new Test() {
      Get(api(s"$path")) ~> route ~> check {
        mediaType ==== HttpApi.opml.mediaType
        status ==== StatusCodes.OK
      }
    }

    "import file to api" in {
      success
    }
  }

  private class Test() extends BaseScope {
    val service = new OpmlService(null) {
      override def build: Task[String] = Task.succeed(opmlContent)

      override def create(text: String): Task[Iterable[SourceViewDto]] = ???
    }
    override protected val route: Route = new OpmlApi(service).route
  }

}
