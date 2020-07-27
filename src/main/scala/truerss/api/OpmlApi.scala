package truerss.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import truerss.dto.SourceViewDto
import truerss.services.OpmlService
import truerss.util.OpmlExtractor

import scala.concurrent.duration._

class OpmlApi(private val opmlService: OpmlService) extends HttpApi {

  import JsonFormats._
  import OpmlExtractor._

  protected val defaultSerializationTime = 3 seconds

  val route = api {
    pathPrefix("opml") {
      (post & pathPrefix("import")) {
        makeImport1
      } ~ (get & pathEndOrSingleSlash) {
        doneWith(opmlService.build, HttpApi.opml)
      }
    }
  }

  protected def makeImport1: Route = {
    extractStrictEntity(defaultSerializationTime) { entity =>
      val text = reprocessToOpml(entity.data.utf8String)
      val result = opmlService.create(text)
      w[Iterable[SourceViewDto]](result)
    }
  }

}
