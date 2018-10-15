package truerss

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import truerss.api.{RoutingApiImpl, WSApi}
import truerss.db._
import truerss.db.drivers.SupportedDb
import truerss.services.MainActor
import truerss.util.TrueRSSConfig

import scala.language.postfixOps

object Boot extends App {

  val parser = TrueRSSConfig.parser

  parser.parse(args, TrueRSSConfig()) match {
    case Some(trueRSSConfig) =>

      val (actualConfig, dbConf, isUserConf) = TrueRSSConfig.loadConfiguration(trueRSSConfig)

      implicit val system = ActorSystem("truerss")
      import system.dispatcher
      implicit val materializer = ActorMaterializer()

      val dbEc = system.dispatchers.lookup("dispatchers.db-dispatcher")

      val dbLayer = SupportedDb.load(dbConf, isUserConf)(dbEc)

      val mainActor = system.actorOf(
        MainActor.props(actualConfig, dbLayer),
        "main-actor"
      )

      Http().bindAndHandle(new RoutingApiImpl(mainActor).route,
        actualConfig.host,
        actualConfig.port
      )

      val socketApi = system.actorOf(WSApi.props(actualConfig.wsPort), "ws-api")


    case None =>
      Console.err.println("Unknown argument")
      sys.exit(1)
  }

}
