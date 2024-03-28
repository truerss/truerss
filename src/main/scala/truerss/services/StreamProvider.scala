package truerss.services

import akka.event.EventStream
import zio.{Task, ZIO}

trait StreamProvider {
  val stream: EventStream
  def fire(x: Any): Task[Unit] = {
    ZIO.environmentWith { _ =>
      stream.publish(x)
    }
  }
}
