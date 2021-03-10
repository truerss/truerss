package truerss.api

import truerss.dto.SourceOverview
import truerss.services.SourceOverviewService
import com.github.fntz.omhs.{BodyWriter, RoutingDSL, Route}
import com.github.fntz.omhs.playjson.JsonSupport

class SourcesOverviewApi(private val sourceOverviewService: SourceOverviewService) extends HttpApi {

  import JsonFormats._
  import ZIOSupport._
  import RoutingDSL._

  implicit val sourceOverviewWriter: BodyWriter[SourceOverview] =
    JsonSupport.writer[SourceOverview]

  private val overview = get("api" / "v1" / "overview" / long) ~> {(sourceId: Long) =>
    sourceOverviewService.getSourceOverview(sourceId)
  }

  val route = new Route().addRule(overview)

}
