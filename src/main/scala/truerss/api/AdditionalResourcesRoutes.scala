package truerss.api

import com.github.fntz.omhs.{AsyncResult, CommonResponse, CurrentHttpRequest, RoutingDSL}
import io.netty.handler.codec.http.cookie.{DefaultCookie, ServerCookieEncoder}
import io.netty.handler.codec.http.{HttpHeaderNames, HttpResponseStatus}
import io.netty.util.CharsetUtil

import scala.io.Source

class AdditionalResourcesRoutes(private val wsPort: Int) {

  import AdditionalResourcesRoutes._
  import RoutingDSL._
  import ZIOSupport._

  protected val fileName = "index.html"
  private val indexStr = Source.fromInputStream(getClass.getResourceAsStream(s"/$fileName")).mkString

  def pass(redirectTo: Option[String]) = {
    val headers = redirectTo.map { location =>
      val cookie = new DefaultCookie(redirectToLocation, location)
      val result = ServerCookieEncoder.STRICT.encode(cookie)
      Map(HttpHeaderNames.COOKIE.toString -> result)
    }.getOrElse(Map.empty)
    AsyncResult.completed(
      CommonResponse(
        status = HttpResponseStatus.OK,
        contentType = "text/html",
        content = indexStr.getBytes(CharsetUtil.UTF_8),
        headers = headers
      )
    )
  }

  private val indexR = get("/") ~> { () =>
    pass(None)
  }

  private val commonHandler = (c: CurrentHttpRequest) => {
    if (c.isXhr) {
      AsyncResult.completed(aboutResponse)
    } else {
      pass(Some(c.path))
    }
  }

  private val about = get("about") ~> {(c: CurrentHttpRequest) => commonHandler(c) }
  private val settings = get("settings") ~> {(c: CurrentHttpRequest) => commonHandler(c) }
  private val favorites = get("favorites") ~> {(c: CurrentHttpRequest) => commonHandler(c) }
  private val show = get("show") ~> {(c: CurrentHttpRequest) => commonHandler(c) }
  private val search = get("search") ~> {(c: CurrentHttpRequest) => commonHandler(c) }

  val route = indexR :: about :: settings :: favorites :: show :: search

}

object AdditionalResourcesRoutes {

  implicit class CurrentHttpRequestExt(val c: CurrentHttpRequest) extends AnyVal {
    def isXhr: Boolean = {
      Option(c.headers.get(xhrHeaderName)).forall(_ == xhrHeaderValue)
    }
  }

  final val portC = "port"
  final val redirectToLocation = "redirectTo"
  final val xhrHeaderName =  "X-Requested-With"
  final val xhrHeaderValue = "XMLHttpRequest"

  final val aboutResponse = CommonResponse(
    status = HttpResponseStatus.OK,
    contentType = "text/html",
    content = Source.fromInputStream(getClass.getResourceAsStream(s"/about.txt"))
      .mkString.getBytes(CharsetUtil.UTF_8)
  )

}
