package truerss.api

import java.nio.charset.Charset

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.model.{ContentType => C, _}
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Failure => F, Success => S}
import play.api.libs.json._
import org.slf4j.LoggerFactory
/**
  * Created by mike on 17.12.16.
  */
trait HttpHelper {

  import JsonFormats._
  import StatusCodes._
  import RouteResult._

  val utf8 = Charset.forName("UTF-8")

  protected val logger = LoggerFactory.getLogger(getClass)

  protected val api = pathPrefix("api" / "v1")

  def create[T : Reads : ClassTag](f: T => Future[Response])(implicit ec: ExecutionContext): Route = {
    entity(as[String]) { json =>
      Json.parse(json).validateOpt[T] match {
        case JsSuccess(Some(value), _) =>
          call(f(value))

        case JsSuccess(_, _) =>
          complete(response(BadRequest, s"Unable to parse request: $json"))

        case JsError(errors) =>
          val str = errors.map(x => s"${x._1}: ${x._2.flatMap(_.messages).mkString(", ")}")
          complete(response(BadRequest, s"Unable to parse request: $str"))
      }
    }
  }

  def call(f: Response)(implicit ec: ExecutionContext): Route = {
    call(Future.successful(f))
  }

  def call(f: Future[Response])(implicit ec: ExecutionContext): Route = {
    onComplete(f.map(responseHandler)) {
      case S(Complete(response)) =>
        complete(response)

      case S(Rejected(_)) =>
        complete(response(InternalServerError, "Rejected"))

      case F(ex) =>
        logger.warn(s"Request failed: $ex")
        complete(response(InternalServerError, "Failed"))
    }
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

  private def responseHandler(x: Response) = {
    x match {
      case SourcesResponse(xs) => ok(xs)
      case SourceResponse(x) => ok(x)
      case SourceOverViewResponse(x) => ok(x)
      case FeedResponse(x) => ok(x)
      case FeedContentResponse(x) => ok(x)
      case FeedsResponse(xs) => ok(xs)
      case FeedsPageResponse(page) => ok(page)
      case AppPluginsResponse(view) => ok(view)
      case ImportResponse(result) => ok(result)
      case SettingsResponse(result) => ok(result)
      case SettingResponse(x) => ok(availableSetupWrites.writes(x)) // todo
      case OpmlResponse(content) =>
        RouteResult.Complete {
          HttpResponse(
            entity = HttpEntity.Chunked.fromData(ContentTypes.`application/octet-stream`,
              Source.single(ByteString(content))))
        }

      case Ok => finish(NoContent, "")

      case CssResponse(content) =>
        flush(ContentTypes.`text/plain(UTF-8)`, content)

      case JsResponse(content) =>
        flush(ContentTypes.`text/plain(UTF-8)`, content)

      case NotFoundResponse(msg) => finish(NotFound, msg)
      case BadRequestResponse(msg) => finish(BadRequest, msg)
      case InternalServerErrorResponse(msg) => finish(InternalServerError, msg)
    }
  }

}

object HttpHelper extends HttpHelper