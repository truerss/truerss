package truerss.controllers

import java.io.StringReader

import akka.pattern.ask
import com.github.fntzr.spray.routing.ext.BaseController
import com.rometools.opml.feed.opml.Opml
import com.rometools.rome.io.WireFeedInput
import org.xml.sax.InputSource
import spray.http.{MultipartFormData, StatusCodes}
import spray.routing.HttpService
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

  def create = entity(as[String]) { sourceString =>
    catching(classOf[Exception])
      .opt((JsonParser(sourceString).convertTo[Source])) match {
      case Some(fs) => end(AddSource(fs.normalize))
      case None => complete(spray.http.StatusCodes.BadRequest, "Not valid data")
    }
  }

  def delete(num: Long) = end(DeleteSource(num))

  def update(num: Long) = entity(as[String]) { sourceString =>
    catching(classOf[Exception])
      .opt((JsonParser(sourceString).convertTo[Source])) match {
      case Some(fs) => end(UpdateSource(num, fs.normalize.copy(id = Some(num))))
      case None => complete(spray.http.StatusCodes.BadRequest, "Not valid data")
    }
  }

  def markAll(num: Long) = end(MarkAll(num))

  def latest(count: Long) = end(Latest(count))

  def feeds(num: Long) = end(ExtractFeedsForSource(num))

  def refresh = end(Update)

  def refreshOne(num: Long) = end(UpdateOne(num))

  def fromFile = {
    entity(as[MultipartFormData]) { formData => c =>
      val interval = 8
      val file = formData.fields.map(_.entity.asString).reduce(_ + _)
      val input = new WireFeedInput()
      val opml = input.build(new InputSource(
        new StringReader(file.replaceAll("[^\\x20-\\x7e]", ""))))
      .asInstanceOf[Opml]
      val result = opml.getOutlines.flatMap(_.getChildren).map { x =>
        (x.getXmlUrl, x.getTitle)
      }.filterNot(x => x._1.isEmpty && x._2.isEmpty).map { case t @ (url, title) =>
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
