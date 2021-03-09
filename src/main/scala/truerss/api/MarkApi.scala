package truerss.api

import truerss.services.MarkService
import com.github.fntz.omhs.macros.RoutingImplicits
import com.github.fntz.omhs.{AsyncResult, BodyWriter, ParamDSL}

class MarkApi(private val markService: MarkService) extends HttpApi {

  import ParamDSL._
  import RoutingImplicits._
  import ZIOSupport._

  private val markAll = put("api" / "v1" / "mark") ~> {() =>
    markService.markAll
  }

  private val markSource = put("api" / "v1" / "mark" / long) ~> { (sourceId: Long) =>
    markService.markOne(sourceId)
  }

  val route = ???

//  val route = api {
//    pathPrefix("mark") {
//      put {
//        pathEndOrSingleSlash {
//          w[Unit](markService.markAll)
//        } ~ pathPrefix(LongNumber) { sourceId =>
//          w[Unit](markService.markOne(sourceId))
//        }
//      }
//    }
//  }

}
