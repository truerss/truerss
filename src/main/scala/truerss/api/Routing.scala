package truerss.api

import akka.actor._
import akka.event.LoggingAdapter

import com.github.fntzr.spray.routing.ext.Routable

import spray.http.StatusCodes
import spray.routing.ExceptionHandler
import spray.json.DeserializationException

import truerss.controllers._

import spray.routing.PathMatchers._
/**
 * Created by mike on 1.8.15.
 */
trait Routing extends Routable {


  def route(proxyRef: ActorRef, context: ActorRefFactory) =
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
            get0[SourceController](("latest"  / LongNumber) ~> "latest") ~
            get0[SourceController](("feeds" / LongNumber) ~> "feeds")
//            put0[SourceController]("refresh") ~
//            put0[SourceController](("refresh" / LongNumber) ~> "refresh_one") ~
//            post0[SourceController]("import" ~> "fromFile")
          } ~ scope("feeds") {
            get0[FeedController]("favorites") ~
            get0[FeedController](LongNumber ~> "show") ~
            put0[FeedController](("mark" / LongNumber) ~> "mark") ~
            put0[FeedController](("unmark" / LongNumber) ~> "unmark") ~
            put0[FeedController](("read" / LongNumber) ~> "read") ~
            put0[FeedController](("unread" / LongNumber) ~> "unread")
          }
        }
      }


}


class RoutingService(proxyRef: ActorRef) extends Actor with Routing with ActorLogging {
  def actorRefFactory = context
  def receive = runRoute(route(proxyRef, context))
}