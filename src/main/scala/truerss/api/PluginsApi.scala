package truerss.api

import truerss.dto.PluginsViewDto
import truerss.services.ApplicationPluginsService
import com.github.fntz.omhs.macros.RoutingImplicits
import com.github.fntz.omhs.{BodyWriter, CommonResponse, ParamDSL}
import com.github.fntz.omhs.playjson.JsonSupport
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.util.CharsetUtil

class PluginsApi(private val pluginsService: ApplicationPluginsService) extends HttpApi {

  import JsonFormats._
  import RoutingImplicits._
  import ParamDSL._
  import ZIOSupport._

  implicit val pluginsViewWriter: BodyWriter[PluginsViewDto] =
    JsonSupport.writer[PluginsViewDto]

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
    ???
  }

  private val css = get(base / "css") ~> { () =>
    pluginsService.css.map { x =>
      CommonResponse(
        status = HttpResponseStatus.OK,
        contentType = "`text/css",
        content = x.getBytes(CharsetUtil.UTF_8)
      )
    }
    ???
  }

  val route = plugins :: js :: css

}
