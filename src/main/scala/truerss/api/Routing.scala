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
            post0[SourceController]("create")
            //put0[SourceController](("mark" / LongNumber) ~> "markAll")// ~
            //get0[SourceController]("latest") ~

//            get0[SourceController](("feeds" / LongNumber) ~> "feeds") ~
//            post0[SourceController]("create") ~
//            put0[SourceController]("refresh") ~
//            put0[SourceController](("refresh" / LongNumber) ~> "refresh_one") ~
//            put0[SourceController](LongNumber ~> "update") ~
//            delete0[SourceController](LongNumber ~> "delete") ~
//            post0[SourceController]("import" ~> "fromFile")
          }
        }
      }


}


class RoutingService(proxyRef: ActorRef) extends Actor with Routing with ActorLogging {
  def actorRefFactory = context
  def receive = runRoute(route(proxyRef, context))
}