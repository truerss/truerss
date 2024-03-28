package truerss.api

import com.github.fntz.omhs.{AsyncResult, BodyWriter, CommonResponse}
import zio.{Task, UIO, Unsafe}

object ZIOSupport {

  import HttpApi._

  private val runtime = zio.Runtime.default

  implicit val unitWriter: BodyWriter[Unit] = (_: Unit) => CommonResponse.empty

  object UIOImplicits {
    implicit def UIO2AsyncResult[T](task: UIO[T])(implicit writer: BodyWriter[T]): AsyncResult = {
      val value = Unsafe.unsafe { implicit unsafe => runtime.unsafe.run(task).getOrThrow() }
      AsyncResult.completed(writer.write(value))
    }
  }

  implicit def taskToAsync[T](task: Task[T])(implicit writer: BodyWriter[T]): AsyncResult = {
    AsyncResult.completed(Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.run(flush(task)).getOrThrow()
    })
  }

}
