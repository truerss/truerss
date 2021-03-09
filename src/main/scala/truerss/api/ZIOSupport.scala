package truerss.api

import com.github.fntz.omhs.{AsyncResult, BodyWriter, CommonResponse}
import zio.{Task, UIO}

object ZIOSupport {

  implicit val unitWriter: BodyWriter[Unit] = new BodyWriter[Unit] {
    override def write(w: Unit): CommonResponse = CommonResponse.empty
  }

  implicit def unit2AsyncResult(value: Unit): AsyncResult = {
    AsyncResult.completed(CommonResponse.empty)
  }

  implicit def taskToAsync[T](task: Task[T])(implicit writer: BodyWriter[T]): AsyncResult = {
    task.toAsync
  }

  implicit class Task2Async[T](val task: Task[T])(implicit writer: BodyWriter[T]) {
    def toAsync: AsyncResult = {
      // todo runAsync
      val value = zio.Runtime.default.unsafeRun(task)
      AsyncResult.completed(writer.write(value))
    }
  }

  implicit class UIO2Async[T](val uio: UIO[T])(implicit writer: BodyWriter[T]) {
    def toAsync: AsyncResult = {
      val value = zio.Runtime.default.unsafeRun(uio)
      AsyncResult.completed(writer.write(value))
    }
  }
}
