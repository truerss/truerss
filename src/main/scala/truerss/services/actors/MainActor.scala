package truerss.services.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.EventStream
import truerss.services.actors.events.{EventHandlerActor, PublishPluginActor}
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.services.{ApplicationPluginsService, FeedsService, SourcesService}
import truerss.util.TrueRSSConfig

class MainActor(config: TrueRSSConfig,
                applicationPluginsService: ApplicationPluginsService,
                sourcesService: SourcesService,
                feedsService: FeedsService)
  extends Actor with ActorLogging {

  private val stream: EventStream = context.system.eventStream

  val eventHandlerActor = create(
    EventHandlerActor.props(sourcesService, feedsService),
    "event-handler-actor")

  val publishActor = create(Props(
    classOf[PublishPluginActor], applicationPluginsService),
    "publish-plugin-actor")

  val sourcesRef = create(SourcesKeeperActor.props(
    SourcesKeeperActor.SourcesSettings(config.feedParallelism),
    applicationPluginsService,
    sourcesService
  ), s"source-keeper-actor")

  stream.subscribe(publishActor, classOf[PublishPluginActor.PublishEvent])
  stream.subscribe(eventHandlerActor, classOf[EventHandlerActor.EventHandlerActorMessage])
  stream.subscribe(sourcesRef, classOf[SourcesKeeperActor.SourcesMessage])

  def receive = {
    case x =>
      log.warning(s"Unhandled message: $x, from: ${sender()}")
  }

  private def create(props: Props, name: String): ActorRef = {
    context.actorOf(props, name)
  }

}

object MainActor {

  def props(config: TrueRSSConfig,
            applicationPluginsService: ApplicationPluginsService,
            sourcesService: SourcesService,
            feedsService: FeedsService) = {
    Props(classOf[MainActor], config, applicationPluginsService,
      sourcesService,
      feedsService)
  }
}
