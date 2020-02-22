package truerss.api

import java.nio.charset.Charset

import akka.actor.ActorRef
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout

import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.reflect.ClassTag
import scala.util.{Try, Failure => F, Success => S}

/**
  * Created by mike on 17.12.16.
  */
trait HttpHelper {

  import JsonFormats._
  import akka.http.scaladsl.model.{ContentType => C, _}
  import StatusCodes._
  import akka.http.scaladsl.server.Directives._
  import akka.http.scaladsl.server._
  import RouteResult._


  val service: ActorRef // proxy service
  implicit val timeout = Timeout(30 seconds) // default
  implicit val ec: ExecutionContext
  implicit val materializer: Materializer

  val utf8 = Charset.forName("UTF-8")

  val api = pathPrefix("api" / "v1")

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

  def safeParse[T : Reads : ClassTag](json: String): Try[T] = {
    Try {
      Json.parse(json).as[T]
    }
  }

  def create[T : Reads : ClassTag](f: T => Any) = {
    entity(as[String]) { json =>
      safeParse[T](json) match {
        case S(dto) =>
          sendAndWait(f(dto))
        case F(x) =>
          complete(response(BadRequest, s"Unable to parse request: $x"))
      }
    }
  }

  def create1[T : Reads : ClassTag](f: T => Future[Response]) = {
    entity(as[String]) { json =>
      safeParse[T](json) match {
        case S(dto) =>
          call(f(dto))
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

  def call(f: Response): Route = {
    call(Future.successful(f))
  }

  def call(f: Future[Response]): Route = {
    andWait(f)
  }

  def andWait(f: Future[Response]): Route = {
    standardComplete(f.map(responseHandler))
  }


  private def ok[T: Writes](x: T) : RouteResult = {
    finish(OK, Json.stringify(Json.toJson(x)))
  }

  private def responseHandler(x: Response) = {
    x match {
      case SourcesResponse(xs) => ok(xs)
      case SourceResponse(x) => ok(x)
      case FeedResponse(x) => ok(x)
      case FeedsResponse(xs) => ok(xs)
      case FeedsPageResponse(xs, total) => ok(xs)
      case AppPluginsResponse(view) => ok(view)
      case ImportResponse(result) => ok(result)

      case Ok(x) => finish(OK, x.toString)

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
