package truerss.services.actors.events

import com.github.truerss.base.Entry
import truerss.dto.SourceViewDto
import truerss.util.TaskImplicits
import truerss.services.{FeedsService, SourceStatusesService, SourcesService, StreamProvider}
import io.truerss.actorika._
import truerss.api.ws.WebSocketController

class EventHandlerActor(private val sourcesService: SourcesService,
                        private val sourcesStatusesService: SourceStatusesService,
                        private val feedsService: FeedsService)
  extends Actor with StreamProvider {

  import EventHandlerActor._
  import TaskImplicits._

  def receive: Receive = {
    case RegisterNewFeeds(sourceId, entries) =>
      val f = for {
        feeds <- feedsService.registerNewFeeds(sourceId, entries)
        _ <- sourcesStatusesService.resetErrors(sourceId)
        _ <- fire(PublishPluginActor.NewEntriesReceived(feeds)).when(feeds.nonEmpty)
        _ <- fire(WebSocketController.NewFeeds(feeds)).when(feeds.nonEmpty)
      } yield ()
      f.materialize

    case ModifySource(sourceId) =>
      sourcesService.changeLastUpdateTime(sourceId).materialize

    case NewSourceCreated(source) =>
      fire(WebSocketController.NewSource(source)).materialize

    case SourceError(sourceId) =>
      sourcesStatusesService.incrementError(sourceId).materialize

  }
}

object EventHandlerActor {
  def props(sourcesService: SourcesService,
            sourcesStatusesService: SourceStatusesService,
            feedsService: FeedsService) = {
    new EventHandlerActor(sourcesService, sourcesStatusesService, feedsService)
  }

  sealed trait EventHandlerActorMessage
  case class RegisterNewFeeds(sourceId: Long, entries: Iterable[Entry]) extends EventHandlerActorMessage
  case class ModifySource(sourceId: Long) extends EventHandlerActorMessage
  case class NewSourceCreated(source: SourceViewDto) extends EventHandlerActorMessage
  case class SourceError(sourceId: Long) extends EventHandlerActorMessage
}