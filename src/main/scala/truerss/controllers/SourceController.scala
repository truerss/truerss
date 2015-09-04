package truerss.controllers

import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import com.github.fntzr.spray.routing.ext.BaseController
import spray.http.HttpRequest
import truerss.models.{FrontendSource, Source, ApiJsonProtocol}
import truerss.system.db
import truerss.system.util

import scala.concurrent.duration._
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
  import spray.json._
  import ApiJsonProtocol._
  import db._
  import util._

  def all = end(GetAll)

  def show(num: Long) = end(GetSource(num))

  def create = entity(as[String]) { sourceString =>
    catching(classOf[spray.json.DeserializationException])
      .opt((JsonParser(sourceString).convertTo[FrontendSource])) match {
      case Some(fs) => end(AddSource(fs.toSource.normalize))
      case None => complete(spray.http.StatusCodes.BadRequest, "Not valid json")
    }
  }

  def delete(num: Long) = end(DeleteSource(num))

  def update(num: Long) = entity(as[String]) { sourceString =>
    catching(classOf[spray.json.DeserializationException])
      .opt((JsonParser(sourceString).convertTo[FrontendSource])) match {
      case Some(fs) => end(UpdateSource(num, fs.toSource.normalize.copy(id = num.some)))
      case None => complete(spray.http.StatusCodes.BadRequest, "Not valid json")
    }
  }

  def markAll(num: Long) = end(MarkAll(num))

  def latest(count: Long) = end(Latest(count))

  def feeds(num: Long) = end(ExtractFeedsForSource(num))

  def refresh = end(Update)

  def refreshOne(num: Long) = end(UpdateOne(num))


}
