package net.truerss

import zio.Task

object ZIOMaterializer {

  implicit class TaskExt[T](val x: Task[T]) extends AnyVal {
    def m: T = zio.Runtime.default.unsafeRunTask(x)

    def e: Either[Throwable, T] = {
      zio.Runtime.default.unsafeRunTask(x.either)
    }

    def err[T <: Throwable] = {
      e.swap.toOption.get.asInstanceOf[T]
    }

  }

}
