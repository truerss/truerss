package truerss

import akka.actor.ActorSystem
import com.github.fntz.omhs.OMHSServer
import org.slf4j.LoggerFactory
import truerss.api.RoutingEndpoint
import truerss.api.ws.SocketServer
import truerss.db.Predefined
import truerss.db.driver.DbInitializer
import truerss.db.validation.{PluginSourceValidator, SourceUrlValidator, SourceValidator}
import truerss.services._
import truerss.services.actors.MainActor
import truerss.util.{DbConfig, PluginInstaller, TaskImplicits, TrueRSSConfig}

import scala.language.postfixOps

object AppRunner {

  import TaskImplicits._

  private val logger = LoggerFactory.getLogger(getClass)

  def run(actualConfig: TrueRSSConfig,
          dbConf: DbConfig,
          isUserConf: Boolean)(implicit actorSystem: ActorSystem): OMHSServer.Instance = {
    val dbLayer = DbInitializer.initialize(dbConf, isUserConf)

    val stream = actorSystem.eventStream

    val settingsService = new SettingsService(dbLayer)
    val sourceOverviewService = new SourceOverviewService(dbLayer)
    val sourceUrlValidator = new SourceUrlValidator()
    val sourceValidator = new SourceValidator(dbLayer, sourceUrlValidator, actualConfig.appPlugins)
    val sourcesService = new SourcesService(dbLayer, actualConfig.appPlugins, stream, sourceValidator)
    val applicationPluginsService = new ApplicationPluginsService(actualConfig.appPlugins)
    val opmlService = new OpmlService(sourcesService)
    val feedsService = new FeedsService(dbLayer)
    val readerClient = new ReaderClient(applicationPluginsService)
    val contentReaderService = new ContentReaderService(feedsService, readerClient)
    val searchService = new SearchService(dbLayer)
    val refreshSourcesService = new RefreshSourcesService(stream)
    val markService = new MarkService(dbLayer)
    val pluginInstaller = new PluginInstaller(actualConfig.pluginsDir)
    val pluginSourcesValidator = new PluginSourceValidator(dbLayer)
    val pluginSourcesService = new PluginSourcesService(dbLayer, pluginInstaller, pluginSourcesValidator)

    val feedParallelism = settingsService.where[Int](
      Predefined.parallelism.toKey,
      Predefined.parallelism.default[Int]
    ).materialize.value

    actorSystem.actorOf(
      MainActor.props(actualConfig.withParallelism(feedParallelism),
        applicationPluginsService, sourcesService,  feedsService),
      "main-actor"
    )

    val endpoint = new RoutingEndpoint(
      feedsService = feedsService,
      sourcesService = sourcesService,
      pluginsManagement = applicationPluginsService,
      searchService = searchService,
      opmlService = opmlService,
      sourceOverviewService = sourceOverviewService,
      settingsService = settingsService,
      contentReaderService = contentReaderService,
      refreshSourcesService = refreshSourcesService,
      markService = markService,
      pluginSourcesService = pluginSourcesService,
      wsPort = actualConfig.wsPort
    )

    val server = OMHSServer.init(
      actualConfig.host,
      actualConfig.port,
      endpoint.route.toHandler,
      None,
      OMHSServer.noServerBootstrapChanges
    )

    val webSocketServer = SocketServer(actualConfig.wsPort, actorSystem)
    webSocketServer.start()
    actorSystem.registerOnTermination {
      webSocketServer.stop()
      server.stop()
      logger.info(s"========> ActorSytem[${actorSystem.name}] is terminating...")
    }
    server
  }
}