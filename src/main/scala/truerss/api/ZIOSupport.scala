package truerss.api

import com.github.fntz.omhs.{AsyncResult, BodyWriter, CommonResponse}
import io.netty.handler.codec.http.HttpResponseStatus
import zio.{Task, UIO}

object ZIOSupport {

  import HttpApi._

  implicit val unitWriter: BodyWriter[Unit] = (_: Unit) => CommonResponse.empty

  object UIOImplicits {
    implicit def UIO2AsyncResult[T](task: UIO[T])(implicit writer: BodyWriter[T]): AsyncResult = {
      val value = zio.Runtime.default.unsafeRun(task)
      AsyncResult.completed(writer.write(value))
    }
  }

  implicit def taskToAsync[T](task: Task[T])(implicit writer: BodyWriter[T]): AsyncResult = {
    AsyncResult.completed(zio.Runtime.default.unsafeRun(flush(task)))
  }

}
