package truerss.api

import akka.http.scaladsl.model.Multipart
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

  protected def makeImport: Route = {
    entity(as[Multipart.FormData]) { formData =>
      val result = for {
        text <- Task.fromFuture { implicit ec =>
          formData.parts.map { p =>
            p.entity.dataBytes.runFold("") { (a, b) =>
              a + b.decodeString(HttpApi.utf8)
            }
          }.runFold("") {
            _ + _
          }
        }
        result <- opmlService.create(text)
      } yield result


      w[Iterable[SourceViewDto]](result)
    }
  }

}
