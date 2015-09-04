package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController
import spray.http.HttpCookie

import spray.routing.HttpService

trait MainController extends BaseController with WsPortProvider {

  import HttpService._
  import spray.httpx.TwirlSupport._

  def root =
    setCookie(HttpCookie("port", content = s"$wsPort")) {
      complete(truerss.html.index.render)
    }


}
