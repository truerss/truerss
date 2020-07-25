package truerss.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import truerss.dto.SourceViewDto
import truerss.services.OpmlService
import zio.Task

class OpmlApi(private val opmlService: OpmlService)
             (private implicit val materializer: Materializer) extends HttpApi {

  import JsonFormats._

  val route = api {
    pathPrefix("opml") {
      (post & pathPrefix("import")) {
        makeImport
      } ~ (get & pathEndOrSingleSlash) {
        doneWith(opmlService.build, HttpApi.opml)
      }
    }
  }

  // todo switch to tasks
  protected def makeImport: Route = {
    fileUpload("import") { case (_, byteSource) =>
      val tmp = byteSource.map { p =>
        p.decodeString(HttpApi.utf8)
      }.runFold("") { _ + _ }.map { text =>
        zio.Runtime.default.unsafeRunTask(opmlService.create(text))
      }(materializer.executionContext)
      w[Iterable[SourceViewDto]](Task.fromFuture { implicit ec => tmp })
    }
  }

}
