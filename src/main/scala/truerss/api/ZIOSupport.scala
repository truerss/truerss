package truerss.api

import com.github.fntz.omhs.{AsyncResult, BodyWriter, CommonResponse}
import zio.Task

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
      ???
    }
  }
}
