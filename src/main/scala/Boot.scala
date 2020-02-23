package truerss

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import truerss.api.{RoutingEndpoint, WebSockersSupport}
import truerss.db.driver.SupportedDb
import truerss.services._
import truerss.services.management.{FeedsManagement, OpmlManagement, PluginsManagement, SourcesManagement}
import truerss.util.TrueRSSConfig

import scala.language.postfixOps

object Boot extends App {

  val parser = TrueRSSConfig.parser

  parser.parse(args, TrueRSSConfig()) match {
    case Some(trueRSSConfig) =>

      val (actualConfig, dbConf, isUserConf) = TrueRSSConfig.loadConfiguration(trueRSSConfig)

      implicit val system: ActorSystem = ActorSystem("truerss")
      import system.dispatcher

      val dbEc = system.dispatchers.lookup("dispatchers.db-dispatcher")

      val dbLayer = SupportedDb.load(dbConf, isUserConf)(dbEc)

      val sourcesService = new SourcesService(dbLayer, trueRSSConfig.appPlugins)
      val applicationPluginsService = new ApplicationPluginsService(trueRSSConfig.appPlugins)
      val opmlService = new OpmlService(sourcesService)
      val feedsService = new FeedsService(dbLayer)
      val contentReaderService = new ContentReaderService(applicationPluginsService)

      val stream = system.eventStream

      val opmlManagement = new OpmlManagement(opmlService, sourcesService, stream)
      val sourcesManagement = new SourcesManagement(sourcesService, opmlService, stream)
      val feedsManagement = new FeedsManagement(feedsService, contentReaderService, stream)
      val pluginsManagement = new PluginsManagement(applicationPluginsService)

      system.actorOf(
        MainActor.props(actualConfig, dbLayer),
        "main-actor"
      )

      val endpoint = new RoutingEndpoint(
        sourcesManagement = sourcesManagement,
        feedsManagement = feedsManagement,
        opmlManagement = opmlManagement,
        pluginsManagement = pluginsManagement,
        wsPort = trueRSSConfig.wsPort
      )

      Http().bindAndHandle(endpoint.route,
        actualConfig.host,
        actualConfig.port
      ).foreach { _ =>
        system.log.info(s"Http Server: ${actualConfig.url}")
      }

      system.actorOf(WebSockersSupport.props(actualConfig.wsPort), "ws-api")

    case None =>
      Console.err.println("Unknown argument")
      sys.exit(1)
  }

}
