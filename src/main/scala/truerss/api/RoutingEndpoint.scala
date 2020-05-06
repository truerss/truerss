package truerss.api

import akka.http.scaladsl.model.headers.{HttpCookie, RawHeader}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import truerss.services.management.{FeedsManagement, OpmlManagement, PluginsManagement, SearchManagement, SettingsManagement, SourcesManagement}

import scala.concurrent.ExecutionContext
import scala.io.Source

class RoutingEndpoint(
                     sourcesManagement: SourcesManagement,
                     feedsManagement: FeedsManagement,
                     opmlManagement: OpmlManagement,
                     pluginsManagement: PluginsManagement,
                     settingsManagement: SettingsManagement,
                     searchManagement: SearchManagement,
                     wsPort: Int
                     )(implicit ec: ExecutionContext,
                       val materializer: Materializer) {

  import RoutingEndpoint._

  val sourcesApi = new SourcesApi(sourcesManagement, feedsManagement, opmlManagement)
  val feedsApi = new FeedsApi(feedsManagement)
  val pluginsApi = new PluginsApi(pluginsManagement)
  val settingsApi = new SettingsApi(settingsManagement)
  val searchApi = new SearchApi(searchManagement)

  val apis = sourcesApi.route ~
    feedsApi.route ~
    pluginsApi.route ~
    settingsApi.route ~
    searchApi.route

  val fileName = "index.html"

  val route = pathEndOrSingleSlash {
    setCookie(HttpCookie("port", s"$wsPort")) {
      complete {
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          Source.fromInputStream(getClass.getResourceAsStream(s"/$fileName")).mkString
        )
      }
    }
  } ~ apis ~ pathPrefix("about") {
    complete {
      about
    }
  } ~ pathPrefix("css") {
    getFromResourceDirectory("css")
  } ~
    pathPrefix("js") {
      getFromResourceDirectory("javascript")
    } ~
    pathPrefix("fonts") {
      getFromResourceDirectory("fonts")
    } ~
    pathPrefix("templates") {
      getFromResourceDirectory("templates")
    } ~ pathPrefix("show" / Segments) { segments =>
    respondWithHeader(makeRedirect(s"/show/${segments.mkString("/")}")) {
      redirect("/", StatusCodes.Found)
    }
  } ~ pathPrefix(Segment) { segment =>
    respondWithHeader(makeRedirect(segment)) {
      redirect("/", StatusCodes.Found)
    }
  }


}

object RoutingEndpoint {
  def makeRedirect(location: String) = {
    RawHeader("Redirect", location)
  }

  final val about =
    """
      <h1>About</h1>
        <p>
          TrueRss is an open-source feed reader with a customizable plugin system for any content (atom, RSS, youtube channels...).
          More info <a href='https://github.com/truerss/truerss'>Github page</a>
          Download plugins: <a href='https://github.com/truerss/plugins/releases'>plugins</a>
        </p>
    """.stripMargin
}
