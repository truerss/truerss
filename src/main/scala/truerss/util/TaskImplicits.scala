package truerss.util

import zio.{Task, Unsafe, ZIO}

object TaskImplicits {

  private val runtime = zio.Runtime.default

  implicit class TaskExt[T](val task: Task[T]) extends AnyVal {
    def materialize: T = {
      Unsafe.unsafe { implicit unsafe => runtime.unsafe.run(task).getOrThrow() }
    }
  }

}
