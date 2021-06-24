package truerss.services

import io.truerss.actorika._
import zio.Task
import scala.reflect.ClassTag

trait StreamProvider { self: Actor =>
  def fire[T](x: T)(implicit _tag: ClassTag[T]): Task[Unit] = {
    Task.fromFunction { _ =>
      self.system.publish(x)
    }
  }
}
