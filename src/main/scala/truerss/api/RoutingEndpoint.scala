package truerss.api

import org.slf4j.LoggerFactory
import truerss.services._
import com.github.fntz.omhs.{AsyncResult, CommonResponse, RoutingDSL}
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.util.CharsetUtil

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
                       refreshSourcesService: RefreshSourcesService,
                       markService: MarkService,
                       wsPort: Int
                     ) {

  import RoutingDSL._
  import AsyncResult.Implicits._

  val sourcesApi = new SourcesApi(feedsService, sourcesService)
  val feedsApi = new FeedsApi(feedsService, contentReaderService)
  val pluginsApi = new PluginsApi(pluginsManagement)
  val settingsApi = new SettingsApi(settingsService)
  val searchApi = new SearchApi(searchService)
  val refreshApi = new RefreshApi(refreshSourcesService)
  val opmlApi = new OpmlApi(opmlService)
  val markApi = new MarkApi(markService)
  val sourcesOverviewApi = new SourcesOverviewApi(sourceOverviewService)

  protected val logger = LoggerFactory.getLogger(getClass)


  val apis = sourcesApi.route :: feedsApi.route :: pluginsApi.route ::
    settingsApi.route :: searchApi.route :: refreshApi.route ::
    opmlApi.route :: markApi.route :: sourcesOverviewApi.route

  val additional = new AdditionalResourcesRoutes(wsPort)

  private val css = get("css" / *) ~> { (xs: List[String]) =>
    serveWith(xs, "css", "text/css")
  }

  private val js = get("js" / *) ~> {(xs: List[String]) =>
    serveWith(xs, "javascript", "application/javascript")
  }

  private val templates = get("templates" / *) ~> {(xs: List[String]) =>
    serveWith(xs, "templates", "application/javascript")
  }

  private val fonts = get("fonts" / *) ~> {(xs: List[String]) =>
    // maybe probeContentType ?
    xs.headOption match {
      case Some(v) if v.endsWith("otf") =>
        serveFile(s"/fonts/$v", "application/vnd.ms-opentype")
      case Some(v) if v.endsWith("eot") =>
        serveFile(s"/fonts/$v","application/vnd.ms-fontobject")
      case Some(v) if v.endsWith("ttf") =>
        serveFile(s"/fonts/$v","font/sfnt")
      case Some(v) if v.endsWith("woff2") =>
        serveFile(s"/fonts/$v","application/octet-stream")
      case _ =>
        notFound("file not found")
    }
  }

  val route = apis :: additional.route :: css :: js :: templates

  private def serveWith(xs: List[String], dir: String, contentType: String): CommonResponse = {
    xs.headOption
      .map(x => serveFile(s"/$dir/$x", contentType))
      .getOrElse(notFound("file not found"))
  }

  private def serveFile(path: String, contentType: String): CommonResponse = {
    val stream = getClass.getResourceAsStream(path)
    val source = Source.fromInputStream(stream)
    val content = try {
      source.mkString.getBytes(CharsetUtil.UTF_8)
    } finally {
      source.close()
      stream.close()
    }
    CommonResponse(
      status = HttpResponseStatus.OK,
      contentType = contentType,
      content = content
    )
  }

  private def notFound(text: String): CommonResponse =
    CommonResponse.plain(404, text)


}
