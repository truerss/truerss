package truerss.util

import zio.Task

object TaskImplicits {

  implicit class TaskExt[T](val task: Task[T]) extends AnyVal {
    def materialize: T = {
      zio.Runtime.default.unsafeRunTask(task)
    }
  }

}
