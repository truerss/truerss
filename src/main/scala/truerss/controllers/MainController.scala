package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController
import spray.http.{HttpCookie, MediaTypes}
import spray.routing.HttpService

trait MainController extends BaseController with WsPortProvider {

  import HttpService._

  val fileName = "index.html"

  def root = {
    setCookie(HttpCookie("port", content = s"$wsPort")) {
      respondWithMediaType(MediaTypes.`text/html`) {
        complete(scala.io.Source.fromInputStream(getClass.getResourceAsStream(s"/$fileName")).mkString)
      }
    }
  }
}
