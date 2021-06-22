package truerss.services

import io.truerss.actorika.ActorSystem
import truerss.services.actors.sync.SourcesKeeperActor
import zio.Task

class RefreshSourcesService(val system: ActorSystem) {

  def refreshSource(sourceId: Long): Task[Unit] = {
    Task.effect(system.publish(SourcesKeeperActor.UpdateOne(sourceId)))
  }

  def refreshAll: Task[Unit] = {
    Task.effect(system.publish(SourcesKeeperActor.Update))
  }

}
