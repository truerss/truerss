package truerss

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import org.slf4j.LoggerFactory
import truerss.api.RoutingEndpoint
import truerss.api.ws.WebSocketsSupport
import truerss.db.Predefined
import truerss.db.driver.DbInitializer
import truerss.db.validation.{SourceUrlValidator, SourceValidator}
import truerss.services._
import truerss.services.actors.MainActor
import truerss.util.{DbConfig, TrueRSSConfig}

import scala.language.postfixOps

object AppRunner {

  private val logger = LoggerFactory.getLogger(getClass)

  def run(actualConfig: TrueRSSConfig,
          dbConf: DbConfig,
          isUserConf: Boolean)(implicit actorSystem: ActorSystem): Unit = {
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
    val contentReaderService = new ContentReaderService(feedsService, readerClient, settingsService)
    val searchService = new SearchService(dbLayer)
    val refreshSourcesService = new RefreshSourcesService(stream)
    val markService = new MarkService(dbLayer)

    val feedParallelism = zio.Runtime.default.unsafeRunTask(
      settingsService.where[Int](
        Predefined.parallelism.toKey,
        Predefined.parallelism.default[Int]
      )
    ).value

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
      wsPort = actualConfig.wsPort
    )

    Http().bindAndHandle(
      endpoint.route,
      actualConfig.host,
      actualConfig.port
    ).foreach { _ =>
      logger.info(s"Http Server: ${actualConfig.url}")
    }(actorSystem.dispatcher)

    actorSystem.actorOf(WebSocketsSupport.props(actualConfig.wsPort), "ws-api")

    actorSystem.registerOnTermination {
      logger.info(s"========> ActorSytem[${actorSystem.name}] is terminating...")
    }
  }
}