package truerss.api

import akka.stream.Materializer
import org.slf4j.LoggerFactory
import truerss.services._
import com.github.fntz.omhs.macros.RoutingImplicits
import com.github.fntz.omhs.ParamDSL

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
                     )(implicit val materializer: Materializer) {

  import RoutingImplicits._
  import ParamDSL._

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

  // todo read resources from dir

  val route = apis :: additional.route
  /*additional.route ~ apis ~ pathPrefix("css") {
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
    } */



}
