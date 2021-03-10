package truerss.api

import truerss.services.MarkService
import com.github.fntz.omhs.RoutingDSL

class MarkApi(private val markService: MarkService) extends HttpApi {

  import RoutingDSL._
  import ZIOSupport._

  private val markAll = put("api" / "v1" / "mark") ~> {() =>
    markService.markAll
  }

  private val markSource = put("api" / "v1" / "mark" / long) ~> { (sourceId: Long) =>
    markService.markOne(sourceId)
  }

  val route = markAll :: markSource


}
