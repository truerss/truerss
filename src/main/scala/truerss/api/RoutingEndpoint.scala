package truerss.api

import org.slf4j.LoggerFactory
import truerss.services._
import com.github.fntz.omhs.{AsyncResult, CommonResponse, RoutingDSL}
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.util.CharsetUtil

import java.io.File
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

  private def getFilesInDirectory(dir: String) = {
    val folder = new File(getClass.getResource(s"$dir").getPath)
    folder.listFiles().map { x =>
      val source = Source.fromFile(x)
      x.getPath -> (try source.mkString.getBytes(CharsetUtil.UTF_8) finally source.close())
    }.toMap
  }

  private val cssFiles = getFilesInDirectory("css")
  private val jsFiles = getFilesInDirectory("js")
  private val fontsFiles = getFilesInDirectory("fonts")
  private val templatesFiles = getFilesInDirectory("templates")

  private def process(map: Map[String, Array[Byte]], xs: List[String], contentType: String): AsyncResult = {
    map.get(xs.headOption.getOrElse("")) match {
      case Some(f) =>
        AsyncResult.completed(CommonResponse(
          status = HttpResponseStatus.OK,
          contentType = contentType,
          content = f
        ))
      case None =>
        AsyncResult.completed(CommonResponse(
          status = HttpResponseStatus.NOT_FOUND,
          contentType = contentType,
          content = "".getBytes(CharsetUtil.UTF_8)
        ))
    }

  }

  private val css = get("css" / *) ~> { (xs: List[String]) =>
    process(cssFiles, xs, "text/css")
  }

  private val js = get("js" / *) ~> {(xs: List[String]) =>
    process(jsFiles, xs, "application/javascript")
  }

  // todo content-type ?
  private val templates = get("templates" / *) ~> {(xs: List[String]) =>
    process(templatesFiles, xs, "application/javascript")
  }

  //todo fonts
  private val fonts = get("templates" / *) ~> {(xs: List[String]) =>
    process(fontsFiles, xs, "application/javascript")
  }


  val route = apis :: additional.route :: css :: js


}
