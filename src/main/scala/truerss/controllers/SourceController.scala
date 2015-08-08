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
import scalaz._
import Scalaz._

/**
 * Created by mike on 1.8.15.
 */
trait SourceController extends BaseController
  with ProxyRefProvider with ActorRefExt with ResponseHelper {

  import HttpService._
  import truerss.controllers.{Response, ModelsResponse, ModelResponse}
  import spray.json._
  import ApiJsonProtocol._
  import db._

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

  def delete(num: Long) = r(DeleteSource(num))

  def update(num: Long) = entity(as[String]) { sourceString =>
    //TODO check if plugin
    //TODO Skip normalized
    catching(classOf[spray.json.DeserializationException])
      .opt((JsonParser(sourceString).convertTo[Source])) match {
      case Some(source) => r(UpdateSource(num, source.normalize.copy(id = num.some)))
      case None => complete(spray.http.StatusCodes.BadRequest, "Not valid json")
    }
  }


}
