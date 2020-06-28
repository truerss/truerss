package truerss.services

import akka.actor.{Actor, ActorLogging, Props}
import akka.event.EventStream
import com.github.truerss.base.Entry
import truerss.api.WebSockerController

class EventHandlerActor(private val sourcesService: SourcesService,
                        feedsService: FeedsService)
  extends Actor with ActorLogging {

  import EventHandlerActor._
  import context.dispatcher

  val stream: EventStream = context.system.eventStream

  def receive: Receive = {
    case RegisterNewFeeds(sourceId, entries) =>
      feedsService.registerNewFeeds(sourceId, entries)
        .map(WebSockerController.NewFeeds)
        .foreach(stream.publish)

    case ModifySource(sourceId) =>
      sourcesService.changeLastUpdateTime(sourceId).foreach(identity)

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