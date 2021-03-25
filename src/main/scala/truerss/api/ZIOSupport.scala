package truerss.api

import com.github.fntz.omhs.{AsyncResult, BodyWriter, CommonResponse}
import io.netty.handler.codec.http.HttpResponseStatus
import zio.{Task, UIO}

object ZIOSupport {

  implicit val unitWriter: BodyWriter[Unit] = (_: Unit) => CommonResponse.empty

  implicit def unit2AsyncResult(value: Unit): AsyncResult = {
    AsyncResult.completed(CommonResponse.empty)
  }

  implicit def taskCRToAsync(task: Task[CommonResponse]): AsyncResult = {
    AsyncResult.completed(zio.Runtime.default.unsafeRun(task))
  }

  implicit def taskToAsync[T](task: Task[T])(implicit writer: BodyWriter[T]): AsyncResult = {
    task.toAsync
  }

  implicit class Task2Async[T](val task: Task[T])(implicit writer: BodyWriter[T]) {
    def toAsync: AsyncResult = {
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
