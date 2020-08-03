package truerss.api

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.io.Source

class AdditionalResourcesRoutes(private val wsPort: Int) {

  import AdditionalResourcesRoutes._

  protected val fileName = "index.html"

  val route = pathEndOrSingleSlash {
    index
  }~ pathPrefix("about" | "settings" | "plugins" | "favorites" | "show" | "search") {
    extractMatchedPath { path =>
      extractUnmatchedPath { unmatched =>
        handleXHR(
          () => complete(about),
          () => index(s"$path$unmatched")
        )
      }
    }
  }

  private def handleXHR(
                 xhr: () => Route,
                 notXhr: () => Route
               ) = optionalHeaderValueByName(xhrHeaderName) {
    case Some(value) if value == xhrHeaderValue =>
      xhr()

    case _ =>
      notXhr()
  }

  private def index: Route = index(None)
  private def index(str: String): Route = index(Some(str))

  private def index(redirectTo: Option[String]): Route = {
    val more = redirectTo.map { x => Seq(HttpCookie(redirectToC, x)) }.getOrElse(Seq.empty)
    setCookie(HttpCookie(portC, s"$wsPort"), more : _*) {
      complete {
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          Source.fromInputStream(getClass.getResourceAsStream(s"/$fileName")).mkString
        )
      }
    }
  }
}

object AdditionalResourcesRoutes {

  final val portC = "port"
  final val redirectToC = "redirectTo"
  final val xhrHeaderName =  "X-Requested-With"
  final val xhrHeaderValue = "XMLHttpRequest"

  final val about =
    Source.fromInputStream(getClass.getResourceAsStream(s"/about.txt")).mkString
}
