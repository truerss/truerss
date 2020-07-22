package net.truerss

import zio.Task

object ZIOMaterializer {

  implicit class TaskExt[T](val x: Task[T]) extends AnyVal {
    def m: T = zio.Runtime.default.unsafeRun(x)

    def e: Either[Throwable, T] = {
      zio.Runtime.default.unsafeRun(x.either)
    }
  }

}
