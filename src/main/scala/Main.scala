import akka.actor.ActorSystem
import truerss.AppRunner
import truerss.util.TrueRSSConfig

import scala.language.postfixOps

object Main extends App {

  val parser = TrueRSSConfig.parser

  parser.parse(args, TrueRSSConfig()) match {
    case Some(trueRSSConfig) =>

      val (actualConfig, dbConf, isUserConf) = TrueRSSConfig.loadConfiguration(trueRSSConfig)

      implicit val system: ActorSystem = ActorSystem("truerss")

      AppRunner.run(actualConfig, dbConf, isUserConf)

    case None =>
      Console.err.println("Unknown argument")
      sys.exit(1)
  }

}
