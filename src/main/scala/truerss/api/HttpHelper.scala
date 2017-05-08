package truerss.api

import java.nio.charset.Charset

import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import truerss.models.ApiJsonProtocol

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.{Try, Failure => F, Success => S}

/**
  * Created by mike on 17.12.16.
  */
trait HttpHelper {

  import ApiJsonProtocol._
  import akka.http.scaladsl.model.{ContentType => C, _}
  import StatusCodes._
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.server._
  import RouteResult._
  import spray.json._

  val service: ActorRef // proxy service
  implicit val timeout = Timeout(30 seconds) // default
  implicit val ec: ExecutionContext
  implicit val materializer: ActorMaterializer

  val utf8 = Charset.forName("UTF-8")

  def response(status: StatusCode, msg: String) = {
    val entity = status match {
      case StatusCodes.OK =>
        HttpEntity.apply(ContentTypes.`application/json`, msg)

      case _ => HttpEntity.apply(ContentTypes.`application/json`, s"""{"error": "$msg"}""")
    }
    HttpResponse(
      status = status,
      entity = entity
    )
  }

  def finish(status: StatusCode, msg: String): RouteResult = {
    RouteResult.Complete(
      response(status, msg)
    )
  }

  def safeParse[T : JsonReader : ClassTag](json: String): Try[T] = {
    Try {
      JsonParser(json).convertTo[T]
    }
  }

  def create[T : JsonReader : ClassTag](f: T => Any) = {
    entity(as[String]) { json =>
      safeParse[T](json) match {
        case S(dto) =>
          sendAndWait(f(dto))
        case F(x) =>
          complete(response(BadRequest, s"Unable to parse request: $x"))
      }
    }
  }

  def flush(cnt: C, content: String) = {
    val entity = if (cnt.binary) {
      HttpEntity.apply(
        cnt,
        content.getBytes(utf8)
      )
    } else {
      HttpEntity.apply(
        contentType = cnt,
        content.getBytes(utf8)
      )
    }

    RouteResult.Complete(
      HttpResponse(
        status = OK,
        entity = entity
      )
    )
  }

  def send(message: Any): Future[Response] = {
    service.ask(message).mapTo[Response]
  }

  def sendAndWait(message: Any): Route = {
    andWait(send(message))
  }

  def andWait(f: Future[Response]): Route = {
    standardComplete(f.map(responseHandler))
  }


  private def responseHandler(x: Response) = {
    x match {
      case ModelsResponse(xs, c) =>
        if (c > 0) {
          //HttpHeaders.RawHeader("XCount", s"$c"))
          finish(OK, xs.toJson.toString)
        } else {
          finish(OK, xs.toJson.toString)
        }
      case ModelResponse(x) => finish(OK, x.toJson.toString)
      case Ok(x) => finish(OK, x.toString)
      case OpmlResponse(content) =>
        flush(ContentTypes.`application/octet-stream`, content)

      case CssResponse(content) =>
        flush(ContentTypes.`text/plain(UTF-8)`, content)

      case JsResponse(content) =>
        flush(ContentTypes.`text/plain(UTF-8)`, content)

      case NotFoundResponse(msg) => finish(NotFound, msg)
      case BadRequestResponse(msg) => finish(BadRequest, msg)
      case InternalServerErrorResponse(msg) => finish(InternalServerError, msg)
    }
  }

  private def standardComplete(f: Future[RouteResult]) = {
    onComplete(f) {
      case S(Complete(response)) =>
        complete(response)

      case S(Rejected(_)) =>
        complete(response(InternalServerError, "Rejected"))

      case F(_) =>
        complete(response(InternalServerError, "Failed"))
    }
  }
}
