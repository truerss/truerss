package truerss.controllers

import akka.util.Timeout
import akka.pattern.ask
import java.io.StringReader

import com.github.fntzr.spray.routing.ext.BaseController
import spray.http.MultipartFormData
import spray.routing.HttpService
import truerss.models.{ApiJsonProtocol, FrontendSource}
import truerss.system.{db, util}

import scala.util.control.Exception._
import scalaz._
import Scalaz._

import com.rometools.opml.feed.opml.Opml
import com.rometools.rome.io.WireFeedInput
import org.xml.sax.InputSource

import scala.concurrent.Future

trait SourceController extends BaseController
  with ProxyRefProvider with ActorRefExt with ResponseHelper {

  import ApiJsonProtocol._
  import HttpService._
  import db._
  import spray.json._
  import util._
  import scala.collection.JavaConversions._
  import context.dispatcher

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
        val s = FrontendSource(url, title, interval)
        (proxyRef ? AddSource(s.toSource.normalize)).mapTo[Response]
      }

      Future.sequence(result).onComplete {
        case _ => //TODO push in stream about errors
          c.complete("ok")
      }
    }
  }


}
