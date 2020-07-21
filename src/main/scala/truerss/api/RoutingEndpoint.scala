package truerss.api

import akka.http.scaladsl.model.headers.{HttpCookie, RawHeader}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.LoggingMagnet
import akka.stream.Materializer
import org.slf4j.LoggerFactory
import truerss.services.{ApplicationPluginsService, ContentReaderService, FeedsService, OpmlService, SearchService, SettingsService, SourceOverviewService, SourcesService}

import scala.concurrent.ExecutionContext
import scala.io.Source

class RoutingEndpoint(
                       feedsService: FeedsService,
                       contentReaderService: ContentReaderService,
                       pluginsManagement: ApplicationPluginsService,
                       sourcesService: SourcesService,
                       searchService: SearchService,
                       sourceOverviewService: SourceOverviewService,
                       settingsService: SettingsService,
                       opmlService: OpmlService,
                       wsPort: Int
                     )(implicit ec: ExecutionContext,
                       val materializer: Materializer) {

  import RoutingEndpoint._

  val sourcesApi = new SourcesApi(
    feedsService, sourcesService, sourceOverviewService, opmlService)
  val feedsApi = new FeedsApi(feedsService, contentReaderService)
  val pluginsApi = new PluginsApi(pluginsManagement)
  val settingsApi = new SettingsApi(settingsService)
  val searchApi = new SearchApi(searchService)

  protected val logger = LoggerFactory.getLogger(getClass)

  private def logIncomingRequest(req: HttpRequest): Unit = {
    logger.debug(s"${req.method.value} ${req.uri}")
  }

  private val log = logRequest(LoggingMagnet(_ => logIncomingRequest))

  val apis = log {
    sourcesApi.route ~
    feedsApi.route ~
    pluginsApi.route ~
    settingsApi.route ~
    searchApi.route
  }

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
          TrueRss is an open-source feed reader with a customizable plugin system for any content (atom, RSS, youtube channels...).
          More info <a href='https://github.com/truerss/truerss'>Github page</a>.
          <br/>
          Download plugins: <a href='https://github.com/truerss/plugins/releases'>plugins</a>
    """.stripMargin
}
