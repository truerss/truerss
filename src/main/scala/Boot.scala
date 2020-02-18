package truerss

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import truerss.api.{RoutingApiImpl, WebSockersSupport}
import truerss.db.driver.SupportedDb
import truerss.services._
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

      val mainActor = system.actorOf(
        MainActor.props(actualConfig, dbLayer),
        "main-actor"
      )

      Http().bindAndHandle(new RoutingApiImpl(mainActor, actualConfig.wsPort).route,
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
