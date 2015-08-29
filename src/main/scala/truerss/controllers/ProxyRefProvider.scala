package truerss.controllers

import akka.actor.{ActorRef, ActorRefFactory}
import akka.pattern.ask
import akka.util.Timeout
import spray.httpx.marshalling.ToResponseMarshallable
import spray.routing.HttpService._
import spray.routing.RequestContext
import truerss.models.ApiJsonProtocol
import truerss.system.BaseMessage

import scala.concurrent.duration._
import scala.language.postfixOps
/**
 * Created by mike on 2.8.15.
 */
trait ProxyRefProvider {
  val proxyRef: akka.actor.ActorRef
  val context: ActorRefFactory
}

trait ResponseHelper { self : ProxyRefProvider with ActorRefExt =>

  import spray.http.MediaTypes.`application/json`

  def end[T <: BaseMessage](msg: BaseMessage) =
    respondWithMediaType(`application/json`) { implicit ctx =>
      proxyRef <| msg
    }

}

trait ActorRefExt { self : ProxyRefProvider =>
  import ApiJsonProtocol._
  import context.dispatcher
  import spray.json._

  implicit val timeout = Timeout(10 seconds)

  implicit class ActorRefExt(ref: ActorRef) {
    import spray.http.StatusCodes._

    def <<|(x: Any) = ref ? x

    def <|(x: BaseMessage)(implicit ctx: RequestContext) =
      (ref ? x).mapTo[Response].map {
        case ModelsResponse(xs) => ctx.complete(OK, xs.toJson.toString)
        case ModelResponse(x) => ctx.complete(OK, x.toJson.toString)
        case OkResponse(x) => ctx.complete(OK, x.toString)
        case NotFoundResponse(msg) => ctx.complete(NotFound, msg)
        case BadRequestResponse(msg) => ctx.complete(BadRequest, msg)
        case InternalServerErrorResponse(msg) => ctx.complete(InternalServerError, msg)
      }
  }
}
