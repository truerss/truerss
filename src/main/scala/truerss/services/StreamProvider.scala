package truerss.services

import akka.event.EventStream
import zio.Task

trait StreamProvider {
  val stream: EventStream
  def fire(x: Any): Task[Unit] = {
    Task.fromFunction { _ =>
      stream.publish(x)
    }
  }
}
