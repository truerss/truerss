package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController
import spray.http.{HttpCookie, MediaTypes}
import spray.routing.HttpService
import scala.io.Source

trait MainController extends BaseController with WsPortProvider with Redirectize {

  import HttpService._

  val fileName = "index.html"

  def root = {
    optionalHeaderValueByName(Redirect) { mbRedirect =>
      setCookie(HttpCookie("port", content = s"$wsPort"),
        HttpCookie(Redirect, content = mbRedirect.getOrElse("/"))
      ) {
        respondWithMediaType(MediaTypes.`text/html`) {
          complete(Source.fromInputStream(getClass.getResourceAsStream(s"/$fileName")).mkString)
        }
      }
    }
  }
}
