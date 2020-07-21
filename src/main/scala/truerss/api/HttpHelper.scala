package truerss.api

import java.nio.charset.Charset

import akka.http.scaladsl.model.{ContentType => C, _}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import org.slf4j.LoggerFactory
import play.api.libs.json._
import zio.Task

import scala.reflect.ClassTag
/**
  * Created by mike on 17.12.16.
  */
trait HttpHelper {

  import RouteResult._
  import StatusCodes._

  val utf8 = Charset.forName("UTF-8")

  protected val logger = LoggerFactory.getLogger(getClass)

  protected val api = pathPrefix("api" / "v1")

  def createTR[T : Reads : ClassTag, R: Writes](f: T => Task[R]): Route = {
    entity(as[String]) { json =>
      Json.parse(json).validateOpt[T] match {
        case JsSuccess(Some(value), _) =>
          taskCall1(f(value))

        case JsSuccess(_, _) =>
          complete(response(BadRequest, s"Unable to parse request: $json"))

        case JsError(errors) =>
          val str = errors.map(x => s"${x._1}: ${x._2.flatMap(_.messages).mkString(", ")}")
          complete(response(BadRequest, s"Unable to parse request: $str"))
      }
    }
  }

  def w[W: Writes](f: Task[W]): Route = taskCall1(f)

  def taskCall1[W: Writes](f: Task[W]): Route = {
    val taskResult = f.map { r =>
      val result = r match {
        case _: Unit =>
          finish(NoContent, "")
        case _ =>
          finish(OK, Json.stringify(Json.toJson(r)))
      }

      result match {
        case Complete(response) =>
          complete(response)
        case Rejected(_) =>
          complete(response(InternalServerError, "Rejected"))
      }
    }
    zio.Runtime.default.unsafeRun(taskResult)
  }



  private def flush(cnt: C, content: String) = {
    val entity = HttpEntity.apply(
      contentType = cnt,
      content.getBytes(utf8)
    )

    RouteResult.Complete(
      HttpResponse(
        status = OK,
        entity = entity
      )
    )
  }

  private def response(status: StatusCode, msg: String) = {
    val entity = status match {
      case StatusCodes.OK =>
        HttpEntity.apply(ContentTypes.`application/json`, msg)

      case StatusCodes.NoContent =>
        HttpEntity.empty(ContentTypes.`application/json`)

      case _ =>
        HttpEntity.apply(ContentTypes.`application/json`, s"""{"error": "$msg"}""")
    }
    HttpResponse(
      status = status,
      entity = entity
    )
  }

  private def finish(status: StatusCode, msg: String): RouteResult = {
    RouteResult.Complete(
      response(status, msg)
    )
  }

  private def ok[T: Writes](x: T) : RouteResult = {
    finish(OK, Json.stringify(Json.toJson(x)))
  }

}

object HttpHelper extends HttpHelper