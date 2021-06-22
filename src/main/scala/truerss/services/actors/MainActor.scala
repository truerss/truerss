package truerss.services.actors

import io.truerss.actorika._
import org.slf4j.LoggerFactory
import truerss.services.actors.events.{EventHandlerActor, PublishPluginActor}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.services.{ApplicationPluginsService, FeedsService, SourceStatusesService, SourcesService}
import truerss.util.TrueRSSConfig

class MainActor(config: TrueRSSConfig,
                applicationPluginsService: ApplicationPluginsService,
                sourcesService: SourcesService,
                feedsService: FeedsService,
                sourcesStatusesService: SourceStatusesService
               )
  extends Actor {

  private val logger = LoggerFactory.getLogger(getClass)

  override def preStart(): Unit = {
    val eventHandlerActor = spawn(
      EventHandlerActor.props(sourcesService, sourcesStatusesService, feedsService),
      "event-handler-actor"
    )

    val publishActor = spawn(new PublishPluginActor(applicationPluginsService),
      "publish-plugin-actor")

    val sourcesRef = spawn(SourcesKeeperActor.props(
      SourcesKeeperActor.SourcesSettings(config.feedParallelism),
      applicationPluginsService,
      sourcesService
    ), "source-keeper-actor")

    system.subscribe(publishActor, classOf[PublishPluginActor.PublishEvent])
    system.subscribe(eventHandlerActor, classOf[EventHandlerActor.EventHandlerActorMessage])
    system.subscribe(sourcesRef, classOf[SourcesKeeperActor.SourcesMessage])
  }



  def receive: Receive = {
    case x =>
      logger.warn(s"Unhandled message: $x, from: $sender")
  }

}

object MainActor {

  def props(config: TrueRSSConfig,
            applicationPluginsService: ApplicationPluginsService,
            sourcesService: SourcesService,
            feedsService: FeedsService,
            sourcesStatusesService: SourceStatusesService
           ) = {
    new MainActor(config, applicationPluginsService,
      sourcesService,
      feedsService,
      sourcesStatusesService
    )
  }
}
