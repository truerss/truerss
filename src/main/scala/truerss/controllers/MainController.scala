package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController
import spray.http.{HttpCookie, MediaTypes}
import spray.routing.HttpService
import scala.io.Source

trait MainController extends BaseController with WsPortProvider with Redirectize {

  import java.net.URLEncoder
  import java.nio.charset.Charset
  val utf8 = Charset.forName("UTF-8").name()
  import HttpService._

  val fileName = "index.html"

  def root = {
    optionalHeaderValueByName(Redirect) { mbRedirect =>
      setCookie(HttpCookie("port", content = s"$wsPort"),
        HttpCookie(Redirect,
          content = URLEncoder.encode(mbRedirect.getOrElse("/"), utf8),
          path = Some("/")
        )
      ) {
        respondWithMediaType(MediaTypes.`text/html`) {
          complete(Source.fromInputStream(getClass.getResourceAsStream(s"/$fileName")).mkString)
        }
      }
    }
  }
}
