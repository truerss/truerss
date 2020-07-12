
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import truerss.api.{RoutingEndpoint, WebSocketsSupport}
import truerss.db.Predefined
import truerss.db.driver.SupportedDb
import truerss.services._
import truerss.services.actors.MainActor
import truerss.services.management._
import truerss.util.TrueRSSConfig

import scala.concurrent.Await
import scala.concurrent.duration._
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

      val dbLayer = SupportedDb.load(dbConf, isUserConf)(dbEc)

      val stream = system.eventStream

      val settingsService = new SettingsService(dbLayer)(servicesEc)
      val sourceOverviewService = new SourceOverviewService(dbLayer)(servicesEc)
      val sourcesService = new SourcesService(dbLayer, actualConfig.appPlugins)(servicesEc)
      val applicationPluginsService = new ApplicationPluginsService(actualConfig.appPlugins)
      val opmlService = new OpmlService(sourcesService, stream)(servicesEc)
      val feedsService = new FeedsService(dbLayer)(servicesEc)
      val contentReaderService = new ContentReaderService(feedsService,
        applicationPluginsService, settingsService)(servicesEc)
      val searchService = new SearchService(dbLayer)(servicesEc)

      val opmlManagement = new OpmlManagement(opmlService)(servicesEc)
      val sourcesManagement = new SourcesManagement(sourcesService,
        opmlService, sourceOverviewService, stream)(servicesEc)
      val feedsManagement = new FeedsManagement(feedsService,
        contentReaderService, stream)(servicesEc)
      val pluginsManagement = new PluginsManagement(applicationPluginsService)
      val settingsManagement = new SettingsManagement(settingsService)(servicesEc)
      val searchManagement = new SearchManagement(searchService)(servicesEc)

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
        sourcesManagement = sourcesManagement,
        feedsManagement = feedsManagement,
        opmlManagement = opmlManagement,
        pluginsManagement = pluginsManagement,
        settingsManagement = settingsManagement,
        searchManagement = searchManagement,
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
