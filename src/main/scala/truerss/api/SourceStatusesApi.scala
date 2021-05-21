package truerss.api

import com.github.fntz.omhs.{Route, RoutingDSL}
import truerss.services.SourceStatusesService

class SourceStatusesApi(private val sourceStatusesService: SourceStatusesService) {

  import OMHSSupport._
  import ZIOSupport._
  import RoutingDSL._

  private val url = "api" / "v1" / "source-statuses"

  private val all = get(url / "all") ~> {() =>
    sourceStatusesService.findAll
  }

  private val one = get(url / long) ~> {(sourceId: Long) =>
    sourceStatusesService.findOne(sourceId)
  }

  val route = new Route().addRules(all, one)

}
