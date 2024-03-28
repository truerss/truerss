package net.truerss

import zio.{Task, Unsafe}

object ZIOMaterializer {

  private val runtime = zio.Runtime.default

  implicit class TaskExt[T](val x: Task[T]) extends AnyVal {
    def m: T = Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.run(x).getOrThrow()
    }

    def e: Either[Throwable, T] = {
      Unsafe.unsafe { implicit unsafe =>
        runtime.unsafe.run(x.either).getOrThrow()
      }
    }

    def err[T <: Throwable]: T = {
      e.swap.toOption.get.asInstanceOf[T]
    }

  }

}
