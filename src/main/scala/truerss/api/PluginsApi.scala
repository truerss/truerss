package truerss.api

import com.github.fntz.omhs.{CommonResponse, RoutingDSL}
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.util.CharsetUtil
import truerss.services.ApplicationPluginsService

class PluginsApi(private val pluginsService: ApplicationPluginsService) {

  import OMHSSupport._
  import RoutingDSL._
  import ZIOSupport.UIOImplicits._

  private val base = "api" / "v1" / "plugins"
  private val plugins = get(base / "all") ~> { () =>
    pluginsService.view
  }

  private val js = get(base / "js") ~> { () =>
    pluginsService.js.map { x =>
      CommonResponse(
        status = HttpResponseStatus.OK,
        contentType = "application/javascript",
        content = x.getBytes(CharsetUtil.UTF_8)
      )
    }
  }

  private val css = get(base / "css") ~> { () =>
    pluginsService.css.map { x =>
      CommonResponse(
        status = HttpResponseStatus.OK,
        contentType = "`text/css",
        content = x.getBytes(CharsetUtil.UTF_8)
      )
    }
  }

  val route = plugins :: js :: css

}
