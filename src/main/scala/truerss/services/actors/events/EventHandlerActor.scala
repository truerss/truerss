package truerss.services.actors.events

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.EventStream
import com.github.truerss.base.Entry
import truerss.api.ws.WebSocketController
import truerss.dto.SourceViewDto
import truerss.services.{FeedsService, SourcesService}
import truerss.util.EventStreamExt

class EventHandlerActor(private val sourcesService: SourcesService,
                        feedsService: FeedsService)
  extends Actor with ActorLogging {

  import EventHandlerActor._
  import EventStreamExt._

  val stream: EventStream = context.system.eventStream

  def receive: Receive = {
    case RegisterNewFeeds(sourceId, entries) =>
      val f = for {
        feeds <- feedsService.registerNewFeeds(sourceId, entries)
        _ <- stream.fire(PublishPluginActor.NewEntriesReceived(feeds)).when(feeds.nonEmpty)
        _ <- stream.fire(WebSocketController.NewFeeds(feeds)).when(feeds.nonEmpty)
      } yield ()
      zio.Runtime.default.unsafeRunTask(f)

    case ModifySource(sourceId) =>
      zio.Runtime.default.unsafeRunTask(
        sourcesService.changeLastUpdateTime(sourceId)
      )

    case NewSourceCreated(source) =>
      zio.Runtime.default.unsafeRunTask(
        stream.fire(WebSocketController.NewSource(source))
      )

  }
}

object EventHandlerActor {
  def props(sourcesService: SourcesService,
            feedsService: FeedsService): Props = {
    Props(classOf[EventHandlerActor], sourcesService, feedsService)
  }

  sealed trait EventHandlerActorMessage
  case class RegisterNewFeeds(sourceId: Long, entries: Iterable[Entry]) extends EventHandlerActorMessage
  case class ModifySource(sourceId: Long) extends EventHandlerActorMessage
  case class NewSourceCreated(source: SourceViewDto) extends EventHandlerActorMessage
}