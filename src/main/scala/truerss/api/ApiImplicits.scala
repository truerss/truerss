package truerss.api

import akka.http.scaladsl.server.Route
import truerss.services.NotFoundError
import zio.Task

import scala.concurrent.{ExecutionContext, Future}

object ApiImplicits {
  implicit def fr2r[T <: Response](f: Future[T])(implicit ec: ExecutionContext): Route = {
    HttpHelper.call(f)
  }

  implicit def fr2r[T <: Response](f: T)(implicit ec: ExecutionContext): Route = {
    HttpHelper.call(f)
  }

  implicit def t2r[T <: Response](f: Task[T]): Route = {
    val p = f.fold(
      {
        case NotFoundError(er) =>
          NotFoundResponse("boom")
        case _ =>
          InternalServerErrorResponse("asd")
      },
      identity
    )
    HttpHelper.taskCall(p)
  }

}
