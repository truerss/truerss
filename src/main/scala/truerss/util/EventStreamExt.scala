package truerss.util

import akka.event.EventStream
import zio.Task

object EventStreamExt {

  implicit class StreamExt(val stream: EventStream) extends AnyVal {
    def fire(x: Any): Task[Unit] = {
      Task.fromFunction { _ =>
        stream.publish(x)
      }
    }
  }

}
