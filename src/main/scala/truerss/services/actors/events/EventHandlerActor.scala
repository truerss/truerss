package truerss.services.actors.events

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.EventStream
import com.github.truerss.base.Entry
import truerss.api.ws.WebSocketController
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
        _ <- stream.fire(WebSocketController.NewFeeds(feeds))
      } yield ()
      zio.Runtime.default.unsafeRunTask(f)

    case ModifySource(sourceId) =>
      val f = for {
        _ <- sourcesService.changeLastUpdateTime(sourceId)
      } yield ()
      zio.Runtime.default.unsafeRunTask(f)

  }
}

object EventHandlerActor {
  def props(sourcesService: SourcesService,
            feedsService: FeedsService): Props = {
    Props(classOf[EventHandlerActor], sourcesService, feedsService)
  }

  sealed trait EventHandlerActorMessage
  case class RegisterNewFeeds(sourceId: Long, entries: Vector[Entry]) extends EventHandlerActorMessage
  case class ModifySource(sourceId: Long) extends EventHandlerActorMessage
}