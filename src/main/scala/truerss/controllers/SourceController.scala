package truerss.controllers

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import com.github.fntzr.spray.routing.ext.BaseController
import spray.http.HttpRequest
import truerss.models.Source
import truerss.system.db
import truerss.models.ApiJsonProtocol
import scala.concurrent.duration._
import scala.language.postfixOps
import spray.httpx.SprayJsonSupport._
import spray.json.DefaultJsonProtocol
import spray.routing.{RequestContext, HttpService}
import scala.concurrent.Future

import scala.util.control.Exception._

/**
 * Created by mike on 1.8.15.
 */
trait SourceController extends BaseController with ProxyRefProvider {

  implicit val timeout = Timeout(10 seconds)
  import context.dispatcher
  import spray.http.MediaTypes.`application/json`
  import HttpService._
  import truerss.controllers.{Response, ModelsResponse, ModelResponse}
  import ApiJsonProtocol._
  import spray.httpx.SprayJsonSupport._
  import spray.json._
  import db._

  implicit class ActorRefExt(ref: ActorRef) {
    import spray.http.StatusCodes._
    def <|(x: BaseMessage)(implicit ctx: RequestContext) =
      (ref ? x).mapTo[Response].map {
        case ModelsResponse(xs) => ctx.complete(OK, xs.toJson.toString)
        case ModelResponse(x) => ctx.complete(OK, x.toJson.toString)
        case NotFoundResponse(msg) => ctx.complete(NotFound, msg)
        case BadRequestResponse(msg) => ctx.complete(BadRequest, msg)
      }
  }

  private def r[T <: BaseMessage](msg: BaseMessage) =
    respondWithMediaType(`application/json`) { implicit ctx =>
      proxyRef <| msg
    }


  def all = r(GetAll)

  def show(num: Long) = r(GetSource(num))

  def create = entity(as[String]) { sourceString =>

    //TODO check if plugin
    //TODO Skip normalized

    catching(classOf[spray.json.DeserializationException])
      .opt((JsonParser(sourceString).convertTo[Source])) match {
      case Some(source) => r(AddSource(source.normalize))
      case None => complete(spray.http.StatusCodes.BadRequest, "Not valid json")
    }



  }


}
