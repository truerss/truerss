package truerss.api

import com.github.fntz.omhs.{AsyncResult, CommonResponse, CurrentHttpRequest, RoutingDSL}
import io.netty.handler.codec.http.cookie.{DefaultCookie, ServerCookieEncoder}
import io.netty.handler.codec.http.{HttpHeaderNames, HttpResponseStatus}
import io.netty.util.CharsetUtil

import scala.io.Source

class RespourcesRoute(private val wsPort: Int) {

  import RespourcesRoute._
  import RoutingDSL._

  protected val fileName = "index.html"
  private val indexStr = Source.fromInputStream(getClass.getResourceAsStream(s"/$fileName")).mkString
  private val iconName = "favicon.ico"
  private val iconData = getClass.getResourceAsStream(s"/$iconName").readAllBytes()

  def pass(redirectTo: Option[String]) = {
    val headers = redirectTo.map { location =>
      val cookie = new DefaultCookie(redirectToLocation, location)
      val result = ServerCookieEncoder.STRICT.encode(cookie)
      Iterable(HttpHeaderNames.SET_COOKIE.toString -> result)
    }.getOrElse(Iterable.empty)
    val wsPorkCookie = ServerCookieEncoder.STRICT.encode(
      new DefaultCookie(s"$portC", wsPort.toString)
    )
    AsyncResult.completed(
      CommonResponse(
        status = HttpResponseStatus.OK,
        contentType = "text/html",
        content = indexStr.getBytes(CharsetUtil.UTF_8),
        headers = headers ++ Iterable(HttpHeaderNames.SET_COOKIE.toString -> wsPorkCookie)
      )
    )
  }

  private val indexR = get("/") ~> { () =>
    pass(None)
  }

  private val faviconR = get("/favicon.ico") ~> { () =>
    AsyncResult.completed(
      CommonResponse(
        status = HttpResponseStatus.OK,
        contentType = "image/x-icon",
        content = iconData,
        headers = Iterable.empty
      )
    )
  }

  private val commonHandler = (c: CurrentHttpRequest) => {
    if (c.isXHR) {
      AsyncResult.completed(aboutResponse)
    } else {
      pass(Some(c.path))
    }
  }

  private val orRoutes = get("about" | "settings" | "plugins" |
    "favorites" | "show" | "search" | "sources") ~> { (_: String, c: CurrentHttpRequest) =>
    commonHandler(c)
  }

  val route = indexR :: faviconR :: orRoutes

}

object RespourcesRoute {

  final val portC = "port"
  final val redirectToLocation = "redirectTo"

  final val aboutResponse = CommonResponse(
    status = HttpResponseStatus.OK,
    contentType = "text/html",
    content = Source.fromInputStream(getClass.getResourceAsStream(s"/about.txt"))
      .mkString.getBytes(CharsetUtil.UTF_8)
  )

}
