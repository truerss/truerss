package truerss.api

import truerss.services.SourceOverviewService
import com.github.fntz.omhs.{RoutingDSL, Route}

class SourcesOverviewApi(private val sourceOverviewService: SourceOverviewService) {

  import OMHSSupport._
  import ZIOSupport._
  import RoutingDSL._

  private val overview = get("api" / "v1" / "overview" / long) ~> {(sourceId: Long) =>
    sourceOverviewService.getSourceOverview(sourceId)
  }

  val route = new Route().addRule(overview)

}
