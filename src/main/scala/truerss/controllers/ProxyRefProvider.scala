package truerss.controllers

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import spray.http.HttpHeaders
import spray.http.HttpHeaders.RawHeader
import spray.routing.HttpService._
import spray.routing.RequestContext
import truerss.models.ApiJsonProtocol
import truerss.system.ApiMessage

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.concurrent.duration._
import scala.language.postfixOps

trait ProxyRefProvider {
  val proxyRef: akka.actor.ActorRef
  val ectx: ExecutionContextExecutor
}

trait WsPortProvider {
  val wsPort: Int
}

trait FilesProvider {
  val jsFiles: Vector[String]
  val cssFiles: Vector[String]
}

trait ResponseHelper { self : ProxyRefProvider with ActorRefExt =>

  import spray.http.MediaTypes.`application/json`

  def end[T <: ApiMessage](msg: ApiMessage) =
    respondWithMediaType(`application/json`) { implicit ctx =>
      proxyRef <| msg
    }

}

trait HttpHelper {

  import ApiJsonProtocol._
  import akka.http.scaladsl.model._
  import StatusCodes._
  import akka.http.scaladsl.server.RouteResult
  import spray.json._

  val service: ActorRef // proxy service
  implicit val timeout = Timeout(30 seconds) // default
  implicit val ec: ExecutionContext

  def finish(status: StatusCode, msg: String) = {
    val enitity = status match {
      case StatusCodes.OK =>
        HttpEntity.apply(ContentTypes.`application/json`, msg)

      case _ => HttpEntity.apply(ContentTypes.`application/json`, s"""{"error": "$msg"}""")
    }
    HttpResponse(
      status = status,
      entity = enitity
    )

    RouteResult.Complete(
      HttpResponse(
        status = status,
        entity = enitity
      )
    )
  }

  def sendAndWait(message: ApiMessage) = {
    service.ask(message).mapTo[Response].map {
      case ModelsResponse(xs, c) =>
        if (c > 0) {
          //HttpHeaders.RawHeader("XCount", s"$c"))
          finish(OK, xs.toJson.toString)
        } else {
          finish(OK, xs.toJson.toString)
        }
      case ModelResponse(x) => finish(OK, x.toJson.toString)
      case OkResponse(x) => finish(OK, x.toString)
      case NotFoundResponse(msg) => finish(NotFound, msg)
      case BadRequestResponse(msg) => finish(BadRequest, msg)
      case InternalServerErrorResponse(msg) => finish(InternalServerError, msg)
    }
  }
}




trait ActorRefExt { self : ProxyRefProvider =>
  import ApiJsonProtocol._
  implicit val ec = ectx
  import spray.json._

  implicit val timeout = Timeout(10 seconds)

  implicit class ActorRefExt(ref: ActorRef) {
    import spray.http.StatusCodes._

    def <<|(x: Any) = ref ? x

    def <|(x: ApiMessage)(implicit ctx: RequestContext) =
      (ref ? x).mapTo[Response].map {
        case ModelsResponse(xs, c) =>
          if (c > 0) {
            ctx.complete(OK, Seq(HttpHeaders.RawHeader("XCount", s"$c")), xs.toJson.toString)
          } else {
            ctx.complete(OK, xs.toJson.toString)
          }
        case ModelResponse(x) => ctx.complete(OK, x.toJson.toString)
        case OkResponse(x) => ctx.complete(OK, x.toString)
        case NotFoundResponse(msg) => ctx.complete(NotFound, msg)
        case BadRequestResponse(msg) => ctx.complete(BadRequest, msg)
        case InternalServerErrorResponse(msg) => ctx.complete(InternalServerError, msg)
      }
  }
}

trait Redirectize {
  val Redirect = "redirect"
  def makeRedirect(location: String) = {
    RawHeader(Redirect, location)
  }
}
