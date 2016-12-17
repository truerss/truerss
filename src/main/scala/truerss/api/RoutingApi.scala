package truerss.api

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import truerss.models.{ApiJsonProtocol, SourceW}
import truerss.system.{db, global, plugins, util}

import scala.concurrent.ExecutionContext
import scala.util.Try
import scala.io.Source


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

  val fileName = "index.html"

  // TODO add root
  // TODO port
  val route = pathEndOrSingleSlash {
    setCookie(HttpCookie("port", "8080")) {
      complete {
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          Source.fromInputStream(getClass.getResourceAsStream(s"/$fileName")).mkString
        )
      }
    }
  } ~ pathPrefix("api" / "v1") {
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
        } ~ (put & pathPrefix("markall")) {
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
  } ~ pathPrefix("about") {
    complete {
      about
    }
  } ~ pathPrefix("css") {
    getFromResourceDirectory("css")
  } ~
    pathPrefix("js") {
      getFromResourceDirectory("javascript")
    } ~
    pathPrefix("fonts") {
      getFromResourceDirectory("fonts")
    } ~
    pathPrefix("templates") {
      getFromResourceDirectory("templates")
    }

  private def ttry(possibleInt: String, recover: Int) =
    Try(possibleInt.toInt).getOrElse(recover)


  def about =
    """
      <h1>About</h1>
        <p>
          TrueRss is open source feed reader with customizable plugin system
          for any content (atom, rss, youtube channels...).
          More info <a href='http://truerss.net'>truerss official site</a>
          Download plugins: <a href='https://github.com/truerss?utf8=%E2%9C%93&query=plugin'>plugins</a>
        </p>
        <ul>
          <li><code>left-arrow</code> - next post</li>
          <li><code>right-arrow</code> - previous post</li>
          <li><code>shift+n</code> - next source</li>
          <li><code>shift+p</code> - previous source</li>
          <li><code>shift+f</code> - mark\\unmark as favorite</li>
          <li><code>shift+m</code> - mark as read</li>
        </ul>
    """.stripMargin

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
