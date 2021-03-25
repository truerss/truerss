package truerss.api

import truerss.services.RefreshSourcesService
import com.github.fntz.omhs.RoutingDSL

class RefreshApi(private val refreshSourcesService: RefreshSourcesService) extends HttpApi {

  import RoutingDSL._
  import ZIOSupport._

  private val base = "api" / "v1" / "refresh"
  private val refreshAll = put(base / "all") ~> { () =>
    w(refreshSourcesService.refreshAll)
  }

  private val refreshOne = put(base / long) ~> { (sourceId: Long) =>
    w(refreshSourcesService.refreshSource(sourceId))
  }

  val route = refreshAll :: refreshOne

}
