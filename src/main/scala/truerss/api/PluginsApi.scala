package truerss.api

import truerss.dto.PluginsViewDto
import truerss.services.ApplicationPluginsService
import com.github.fntz.omhs.{BodyWriter, CommonResponse, RoutingDSL}
import com.github.fntz.omhs.playjson.JsonSupport
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.util.CharsetUtil

class PluginsApi(private val pluginsService: ApplicationPluginsService) extends HttpApi {

  import JsonFormats._
  import RoutingDSL._
  import ZIOSupport._

  implicit val c2c: BodyWriter[CommonResponse] = new BodyWriter[CommonResponse] {
    override def write(w: CommonResponse): CommonResponse = w
  }

  implicit val pluginsViewWriter: BodyWriter[PluginsViewDto] =
    JsonSupport.writer[PluginsViewDto]

  private val base = "api" / "v1" / "plugins"
  private val plugins = get(base / "all") ~> { () =>
    w(pluginsService.view)
  }

  private val js = get(base / "js") ~> { () =>
    w(
      pluginsService.js.map { x =>
        CommonResponse(
          status = HttpResponseStatus.OK,
          contentType = "application/javascript",
          content = x.getBytes(CharsetUtil.UTF_8)
        )
      }
    )
  }

  private val css = get(base / "css") ~> { () =>
    w(
      pluginsService.css.map { x =>
        CommonResponse(
          status = HttpResponseStatus.OK,
          contentType = "`text/css",
          content = x.getBytes(CharsetUtil.UTF_8)
        )
      }
    )
  }

  val route = plugins :: js :: css

}
