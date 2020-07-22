package truerss.services

import akka.event.EventStream
import truerss.services.actors.sync.SourcesKeeperActor
import zio.Task

class RefreshSourcesService(private val stream: EventStream) {

  def refreshSource(sourceId: Long): Task[Unit] = {
    Task.effectTotal(stream.publish(SourcesKeeperActor.UpdateOne(sourceId)))
  }

  def refreshAll: Task[Unit] = {
    Task.effectTotal(stream.publish(SourcesKeeperActor.Update))
  }

}
