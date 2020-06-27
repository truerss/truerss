package truerss.api

import java.nio.charset.Charset

import akka.stream.Materializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.model.{ContentType => C, _}
import akka.http.scaladsl.server.directives.BasicDirectives.extractRequestContext
import akka.http.scaladsl.server.directives.LoggingMagnet
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.{Try, Failure => F, Success => S}
import play.api.libs.json._
import org.slf4j.LoggerFactory
/**
  * Created by mike on 17.12.16.
  */
trait HttpHelper {

  import JsonFormats._
  import StatusCodes._
  import RouteResult._

//  implicit val ec: ExecutionContext
//  implicit val materializer: Materializer

  val utf8 = Charset.forName("UTF-8")

  protected val logger = LoggerFactory.getLogger(getClass)

  private def logIncomingRequest(req: HttpRequest): Unit = {
    logger.debug(s"[${req.method}] ${req.uri}")
  }

  val log = logRequest(LoggingMagnet(_ => logIncomingRequest))

  val api = pathPrefix("api" / "v1")

  def response(status: StatusCode, msg: String) = {
    val entity = status match {
      case StatusCodes.OK =>
        HttpEntity.apply(ContentTypes.`application/json`, msg)

      case StatusCodes.NoContent =>
        HttpEntity.empty(ContentTypes.`application/json`)

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

  def finish(status: StatusCode): RouteResult = {
    RouteResult.Complete(
      response(status, "")
    )
  }

  def safeParse[T : Reads : ClassTag](json: String): Try[T] = {
    Try {
      Json.parse(json).as[T]
    }
  }

  def create[T : Reads : ClassTag](f: T => Future[Response])(implicit ec: ExecutionContext) = {
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

  def call(f: Response)(implicit ec: ExecutionContext): Route = {
    call(Future.successful(f))
  }

  def call(f: Future[Response])(implicit ec: ExecutionContext): Route = {
    andWait(f)
  }

  def andWait(f: Future[Response])(implicit ec: ExecutionContext): Route = {
    standardComplete(f.map(responseHandler))
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

  private def standardComplete(f: Future[RouteResult]) = {
    onComplete(f) {
      case S(Complete(response)) =>
        complete(response)

      case S(Rejected(_)) =>
        complete(response(InternalServerError, "Rejected"))

      case F(ex) =>
        logger.warn(s"Request failed: $ex")
        complete(response(InternalServerError, "Failed"))
    }
  }
}

object HttpHelper extends HttpHelper