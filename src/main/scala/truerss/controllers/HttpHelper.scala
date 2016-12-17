package truerss.controllers

import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask

import truerss.models.ApiJsonProtocol
import truerss.system.ApiMessage

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import scala.util.{Success => S, Failure => F}

/**
  * Created by mike on 17.12.16.
  */
trait HttpHelper {

  import ApiJsonProtocol._
  import akka.http.scaladsl.model._
  import akka.http.scaladsl.server.Directives._
  import StatusCodes._
  import akka.http.scaladsl.server._
  import RouteResult._
  import spray.json._

  val service: ActorRef // proxy service
  implicit val timeout = Timeout(30 seconds) // default
  implicit val ec: ExecutionContext

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

  def sendAndWait(message: ApiMessage): Route = {
    val f = service.ask(message).mapTo[Response].map {
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

    onComplete(f) {
      case S(Complete(response)) =>
        complete(response)

      case S(Rejected(rejections)) =>
        complete(response(InternalServerError, "Rejected"))


      case F(fail) =>
        complete(response(InternalServerError, "Failed"))

    }
  }
}
