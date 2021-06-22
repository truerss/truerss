package truerss.services

import io.truerss.actorika._
import zio.Task
import scala.reflect.runtime.universe._

trait StreamProvider { self: Actor =>
  def fire[T](x: T)(implicit _tag: TypeTag[T]): Task[Unit] = {
    Task.fromFunction { _ =>
      self.system.publish(x)
    }
  }
}
