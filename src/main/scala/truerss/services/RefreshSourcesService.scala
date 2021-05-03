package truerss.services

import akka.event.EventStream
import truerss.services.actors.sync.SourcesKeeperActor
import zio.Task

class RefreshSourcesService(override val stream: EventStream) extends StreamProvider {

  def refreshSource(sourceId: Long): Task[Unit] = {
    fire(SourcesKeeperActor.UpdateOne(sourceId))
  }

  def refreshAll: Task[Unit] = {
    fire(SourcesKeeperActor.Update)
  }

}
