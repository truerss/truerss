
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import truerss.api.{RoutingEndpoint, WebSocketsSupport}
import truerss.db.Predefined
import truerss.db.driver.DbInitializer
import truerss.db.validation.{SourceUrlValidator, SourceValidator}
import truerss.services._
import truerss.services.actors.MainActor
import truerss.util.TrueRSSConfig

import scala.language.postfixOps

object Main extends App {

  val parser = TrueRSSConfig.parser

  parser.parse(args, TrueRSSConfig()) match {
    case Some(trueRSSConfig) =>

      val (actualConfig, dbConf, isUserConf) = TrueRSSConfig.loadConfiguration(trueRSSConfig)

      implicit val system: ActorSystem = ActorSystem("truerss")
      import system.dispatcher

      val dbEc = system.dispatchers.lookup("dispatchers.db-dispatcher")
      val servicesEc = system.dispatchers.lookup("dispatchers.services-dispatcher")

      val dbLayer = DbInitializer.initialize(dbConf, isUserConf)(dbEc)

      val stream = system.eventStream

      val settingsService = new SettingsService(dbLayer)
      val sourceOverviewService = new SourceOverviewService(dbLayer)(servicesEc)
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

      val feedParallelism = zio.Runtime.default.unsafeRun(
        settingsService.where[Int](
          Predefined.parallelism.toKey, 100
        )
      ).value

      system.actorOf(
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
        system.log.info(s"Http Server: ${actualConfig.url}")
      }

      system.actorOf(WebSocketsSupport.props(actualConfig.wsPort), "ws-api")

    case None =>
      Console.err.println("Unknown argument")
      sys.exit(1)
  }

}
