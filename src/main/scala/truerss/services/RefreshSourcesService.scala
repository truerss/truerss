package truerss.services

import akka.event.EventStream
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.util.EventStreamExt
import zio.Task

class RefreshSourcesService(private val stream: EventStream) {

  import EventStreamExt._

  def refreshSource(sourceId: Long): Task[Unit] = {
    stream.fire(SourcesKeeperActor.UpdateOne(sourceId))
  }

  def refreshAll: Task[Unit] = {
    stream.fire(SourcesKeeperActor.Update)
  }

}
