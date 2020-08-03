package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.dto.SourceOverview
import truerss.services.SourceOverviewService

class SourcesOverviewApi(private val sourceOverviewService: SourceOverviewService) extends HttpApi {

  import JsonFormats._

  val route = api {
    pathPrefix("overview" / LongNumber) { sourceId =>
      w[SourceOverview](sourceOverviewService.getSourceOverview(sourceId))
    }
  }

}
