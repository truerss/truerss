package truerss.services.actors.events

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.EventStream
import com.github.truerss.base.Entry
import truerss.api.WebSockerController
import truerss.services.{FeedsService, SourcesService}

class EventHandlerActor(private val sourcesService: SourcesService,
                        feedsService: FeedsService)
  extends Actor with ActorLogging {

  import EventHandlerActor._
  import context.dispatcher

  val stream: EventStream = context.system.eventStream

  def receive: Receive = {
    case RegisterNewFeeds(sourceId, entries) =>
      feedsService.registerNewFeeds(sourceId, entries)
        .map(WebSockerController.NewFeeds).map { x =>
        stream.publish(x)
      }

    case ModifySource(sourceId) =>
      for {
        _ <- sourcesService.changeLastUpdateTime(sourceId)
      } yield ()

  }
}

object EventHandlerActor {
  def props(sourcesService: SourcesService,
            feedsService: FeedsService) = {
    Props(classOf[EventHandlerActor], sourcesService, feedsService)
  }

  sealed trait EventHandlerActorMessage
  case class RegisterNewFeeds(sourceId: Long, entries: Vector[Entry]) extends EventHandlerActorMessage
  case class ModifySource(sourceId: Long) extends EventHandlerActorMessage
}