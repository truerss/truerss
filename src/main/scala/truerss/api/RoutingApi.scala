package truerss.api

import akka.actor.ActorRef
import akka.http.scaladsl.model.headers.{HttpCookie, RawHeader}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import truerss.models.{JsonFormats, SourceW}
import truerss.services.actors.OpmlActor

import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.util.Try


class RoutingApiImpl(override val service: ActorRef)(
                    implicit override val ec: ExecutionContext,
                    override val materializer: ActorMaterializer
)
  extends RoutingApi with HttpHelper

trait RoutingApi { self: HttpHelper =>

  import JsonFormats._
  import RoutingApi._
  import truerss.services.SourcesActor._
  import truerss.services.actors.FeedsManagementActor._
  import truerss.services.actors.PluginManagementActor._
  import truerss.services.actors.SourcesManagementActor._
  import OpmlActor._

  val fileName = "index.html"

  val route = pathEndOrSingleSlash {
    setCookie(HttpCookie("port", "8081")) {
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
            sendAndWait(ExtractFeedsForSource(sourceId, safeToInt(from, 0), safeToInt(limit, 100)))
          }
        } ~ (put & pathPrefix("refresh" / LongNumber)) { sourceId =>
          sendAndWait(UpdateOne(sourceId))
        } ~ (put & pathPrefix("refresh")) {
          sendAndWait(Update)
        } ~ (post & pathPrefix("import")) {
          fromFile
        } ~ (get & pathPrefix("opml")) {
          sendAndWait(GetOpml)
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
    } ~ pathPrefix("show" / Segments) { segments =>
      respondWithHeader(makeRedirect(s"/show/${segments.mkString("/")}")) {
        redirect("/", StatusCodes.Found)
      }
    } ~ pathPrefix(Segment) { segment =>
      respondWithHeader(makeRedirect(segment)) {
        redirect("/", StatusCodes.Found)
      }
    }


  def fromFile = {
    entity(as[Multipart.FormData]) { formData =>
      val r = formData.parts.mapAsync(1) { p =>
        p.entity.dataBytes.runFold("") { (a, b) =>
          a + b.decodeString(utf8)
        }
      }.runFold("") { _ + _ }.flatMap { x =>
        send(CreateOpmlFromFile(x))
      }

      andWait(r)
    }
  }



}

object RoutingApi {
  final val about =
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

  def safeToInt(possibleInt: String, recover: Int) =
    Try(possibleInt.toInt).getOrElse(recover)

  def makeRedirect(location: String) = {
    RawHeader("Redirect", location)
  }
}