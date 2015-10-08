package truerss.controllers

import java.io.StringReader

import akka.pattern.ask
import com.github.fntzr.spray.routing.ext.BaseController
import com.rometools.opml.feed.opml.Opml
import com.rometools.rome.io.WireFeedInput
import org.xml.sax.InputSource
import spray.http.{MultipartFormData, StatusCodes}
import spray.routing.{Route, HttpService}
import truerss.models.{ApiJsonProtocol, Source, SourceHelper}
import truerss.system.{db, util}
import truerss.util.Lens

import scala.concurrent.Future
import scala.util.control.Exception._
import scala.util.{Failure => F, Success => S}

trait SourceController extends BaseController
  with ProxyRefProvider with ActorRefExt with ResponseHelper {

  import ApiJsonProtocol._
  import HttpService._
  import context.dispatcher
  import db._
  import spray.json._
  import util._
  import Lens._

  import scala.collection.JavaConversions._

  def all = end(GetAll)

  def show(num: Long) = end(GetSource(num))

  private def addOrUpdate(s: String)(f: Source => Route) = {
    catching(classOf[Exception])
      .opt(JsonParser(s).convertTo[Source]) match {
      case Some(fs) => f(fs)
      case None => complete(spray.http.StatusCodes.BadRequest, "Not valid data")
    }
  }

  def create = entity(as[String]) { sourceString =>
    addOrUpdate(sourceString) { source => end(AddSource(source.normalize)) }
  }

  def update(num: Long) = entity(as[String]) { sourceString =>
    addOrUpdate(sourceString) { source =>
      end(UpdateSource(num, source.normalize.newId(num)))
    }
  }

  def delete(num: Long) = end(DeleteSource(num))

  def markAll(num: Long) = end(MarkAll(num))

  def latest(count: Long) = end(Latest(count))

  def feeds(num: Long) = end(ExtractFeedsForSource(num))

  def refresh = end(Update)

  def refreshOne(num: Long) = end(UpdateOne(num))

  def unread(sourceId: Long) = end(Unread(sourceId))

  def fromFile = {
    entity(as[MultipartFormData]) { formData => c =>
      val interval = 8
      val file = formData.fields.map(_.entity.asString).reduce(_ + _)
      val input = new WireFeedInput()
      val opml = input.build(new InputSource(
        new StringReader(file.replaceAll("[^\\x20-\\x7e]", ""))))
      .asInstanceOf[Opml]
      val result = opml.getOutlines.flatMap(_.getChildren).map { x =>
        (Option(x.getXmlUrl), Option(x.getTitle))
      }.collect {
        case p @ (Some(url), Some(title)) => Some((url, title))
        case _ => None
      }.flatMap(identity(_)).map { case t @ (url, title) =>
        val s = SourceHelper.from(url, title, interval)
        (proxyRef ? AddSource(s.normalize)).mapTo[Response]
      }

      Future.sequence(result).onComplete {
        case S(xs) =>
          xs.foreach {
            case BadRequestResponse(msg) =>
              proxyRef ! Notify(NotifyLevels.Danger, msg)
            case _ =>
          }
          c.complete(StatusCodes.OK, "ok")
        case F(error) =>
          proxyRef ! Notify(NotifyLevels.Danger, s"Error when import file ${error.getMessage}")
          c.complete(StatusCodes.BadRequest, "oops")
      }
    }
  }


}
