package truerss.api

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._
import truerss.models.{ApiJsonProtocol, SourceW}
import truerss.system.{db, global, plugins, util}

import scala.concurrent.ExecutionContext
import scala.util.Try


class RoutingApiImpl(override val service: ActorRef)(
                    implicit override val ec: ExecutionContext
)
  extends RoutingApi with HttpHelper

trait RoutingApi { self: HttpHelper =>

  import db._
  import util._
  import plugins._
  import global._
  import spray.json._
  import ApiJsonProtocol._

  // TODO add root
  val route = pathPrefix("api" / "v1") {
    pathPrefix("sources") {
      (get & pathPrefix("all")) {
        sendAndWait(GetAll)
      } ~ (get & pathPrefix(LongNumber)) { sourceId =>
        sendAndWait(GetSource(sourceId))
      } ~ (post & pathEndOrSingleSlash) {
        create[SourceW](x => AddSource(x.toSource))
      } ~ (delete & pathPrefix(LongNumber)) { sourceId =>
        sendAndWait(DeleteSource(sourceId))
      } ~ (put & pathPrefix(LongNumber)) { sourceId =>
        create[SourceW](x => UpdateSource(sourceId, x.toSource))
      } ~ (put & pathPrefix("markAll")) {
         sendAndWait(MarkAll)
      } ~ (put & pathPrefix("mark" / LongNumber)) { sourceId =>
         sendAndWait(Mark(sourceId))
      } ~ (get & pathPrefix("unread" / LongNumber)) { sourceId =>
         sendAndWait(Unread(sourceId))
      } ~ (get & pathPrefix("latest" / LongNumber)) { count =>
         sendAndWait(Latest(count))
      } ~ (get & pathPrefix("feeds" / LongNumber)) { sourceId =>
        parameters('from ? "0", 'limit ? "100") { (from, limit) =>
          sendAndWait(ExtractFeedsForSource(sourceId, ttry(from, 0), ttry(limit, 100)))
        }
      } ~ (put & pathPrefix("refresh" / LongNumber)) { sourceId =>
        sendAndWait(UpdateOne(sourceId))
      } ~ (put & pathPrefix("refresh")) {
        sendAndWait(Update)
      } ~ (post & pathPrefix("import")) {
        complete("import")
      } ~ (get & pathPrefix("opml")) {
        sendAndWait(Opml)
      }
    } ~ pathPrefix("feeds") {
      (get & pathPrefix("favorites")) {
        sendAndWait(Favorites)
      } ~ (get & pathPrefix(LongNumber)) { feedId =>
        sendAndWait(GetFeed(feedId))
      } ~ put {
        pathPrefix("mark" / LongNumber) { feedId =>
          sendAndWait(MarkFeed(feedId))
        } ~ pathPrefix("unmark" / LongNumber) { feedId =>
          sendAndWait(UnmarkFeed(feedId))
        } ~ pathPrefix("read" / LongNumber) { feedId =>
          sendAndWait(MarkAsReadFeed(feedId))
        } ~ pathPrefix("unread" / LongNumber) { feedId =>
          sendAndWait(MarkAsUnreadFeed(feedId))
        }
      }
    } ~ pathPrefix("plugins") {
      get {
        pathPrefix("all") {
          sendAndWait(GetPluginList)
        } ~ pathPrefix("js") {
          sendAndWait(GetJs)
        } ~ pathPrefix("css") {
          sendAndWait(GetCss)
        }
      }
    } ~ pathPrefix("system") {
      get {
        pathPrefix("stop") {
          sendAndWait(StopSystem)
        } ~ pathPrefix("restart") {
          sendAndWait(RestartSystem)
        } ~ pathPrefix("exit") {
          sendAndWait(StopApp)
        }
      }
    }
  }

  private def ttry(possibleInt: String, recover: Int) =
    Try(possibleInt.toInt).getOrElse(recover)


  /*
  def fromFile = {
    entity(as[MultipartFormData]) { formData => c =>
      val interval = 8
      val file = formData.fields.map(_.entity.asString(HttpCharsets.`UTF-8`))
        .reduce(_ + _)
      OpmlParser.parse(file).fold(
        err => {
          proxyRef ! Notify(NotifyLevels.Danger, s"Error when import file $err")
          c.complete(StatusCodes.BadRequest, err)
        },
        xs => {
          val result = xs.map { x =>
            SourceHelper.from(x.link, x.title, interval)
          }.map(s => (proxyRef ? AddSource(s.normalize)).mapTo[Response])
          Future.sequence(result).onComplete {
            case S(seq) =>
              seq.foreach {
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
      )
    }
  }
   */

}
