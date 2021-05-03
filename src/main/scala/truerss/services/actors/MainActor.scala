package truerss.services.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Kill, Props}
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

  private var sourcesRef: ActorRef = context.system.deadLetters
  private var index = 0

  val eventHandlerActor = create(
    EventHandlerActor.props(sourcesService, feedsService),
    "event-handler-actor")

  val publishActor = create(Props(
    classOf[PublishPluginActor], applicationPluginsService),
    "publish-plugin-actor")

  stream.subscribe(publishActor, classOf[PublishPluginActor.PublishEvent])
  stream.subscribe(eventHandlerActor, classOf[EventHandlerActor.EventHandlerActorMessage])

  stream.subscribe(self, classOf[MainActor.MainActorMessage])

  private def startSourcesKeeperActor(): Unit = {
    index = index + 1
    sourcesRef = create(SourcesKeeperActor.props(
      SourcesKeeperActor.SourcesSettings(config.feedParallelism),
      applicationPluginsService,
      sourcesService
    ), s"source-keeper-actor-$index")

    stream.subscribe(sourcesRef, classOf[SourcesKeeperActor.SourcesMessage])
  }

  startSourcesKeeperActor()

  def receive = {
    case MainActor.Restart =>
      log.info("Restart SourcesKeeperActor")
      sourcesRef ! Kill
      startSourcesKeeperActor()

    case x =>
      log.warning(s"Unhandled message: $x, from: ${sender()}")
  }

  private def create(props: Props, name: String): ActorRef = {
    context.actorOf(props, name)
  }

}

object MainActor {

  sealed trait MainActorMessage
  case object Restart extends MainActorMessage

  def props(config: TrueRSSConfig,
            applicationPluginsService: ApplicationPluginsService,
            sourcesService: SourcesService,
            feedsService: FeedsService) = {
    Props(classOf[MainActor], config, applicationPluginsService,
      sourcesService,
      feedsService)
  }
}
