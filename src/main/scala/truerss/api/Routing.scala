package truerss.api

import akka.actor._
import akka.event.LoggingReceive

import com.github.fntzr.spray.routing.ext.Routable
import spray.http.HttpHeaders.RawHeader

import spray.http.{HttpHeaders, HttpCookie, StatusCodes}
import spray.routing.PathMatcher

import truerss.controllers._


trait Routing extends Routable with Redirectize {

  def route(
             proxyRef: ActorRef,
             context: ActorRefFactory,
             wsPort: Int,
             jsFiles: Vector[String],
             cssFiles: Vector[String]
           ): spray.routing.Route =
    root[MainController]("root") ~
      scope("api") {
        scope("v1") {
          scope("sources") {
            get0[SourceController]("all" ~> "all") ~
            get0[SourceController](LongNumber ~> "show") ~
            post0[SourceController]("create") ~
            delete0[SourceController](LongNumber ~> "delete") ~
            put0[SourceController](LongNumber ~> "update") ~
            put0[SourceController](("mark" / LongNumber) ~> "markAll") ~
            get0[SourceController](("unread" / LongNumber) ~> "unread") ~
            get0[SourceController](("latest"  / LongNumber) ~> "latest") ~
            get0[SourceController](("feeds" / LongNumber) ~> "feeds") ~
            put0[SourceController](("refresh" / LongNumber) ~> "refreshOne") ~
            put0[SourceController]("refresh") ~
            post0[SourceController]("import" ~> "fromFile")
          } ~ scope("feeds") {
            get0[FeedController]("favorites") ~
            get0[FeedController](LongNumber ~> "show") ~
            put0[FeedController](("mark" / LongNumber) ~> "mark") ~
            put0[FeedController](("unmark" / LongNumber) ~> "unmark") ~
            put0[FeedController](("read" / LongNumber) ~> "read") ~
            put0[FeedController](("unread" / LongNumber) ~> "unread")
          } ~ scope("plugins") {
            get0[PluginController]("all") ~
            get0[PluginController]("js") ~
            get0[PluginController]("css")
          } ~ scope("system") {
            get0[SystemController]("stop") ~
            get0[SystemController]("restart") ~
            get0[SystemController]("exit")
          }
        }
      } ~ get0[MainController]("about") ~
      pathPrefix("css") {
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


}


class RoutingService(proxyRef: ActorRef, wsPort: Int,
                     jsFiles: Vector[String], cssFiles: Vector[String])
  extends Actor with Routing with ActorLogging {
  def actorRefFactory = context
  def receive = LoggingReceive { runRoute(route(proxyRef,
    context, wsPort, jsFiles, cssFiles)) }
}