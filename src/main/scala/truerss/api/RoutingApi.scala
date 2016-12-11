package truerss.api

import akka.actor.ActorRef
import akka.http.scaladsl.server.Directives._


class RoutingApiImpl extends RoutingApi

trait RoutingApi {

//  def service: ActorRef
  // TODO add root
  val route = pathPrefix("api" / "v1") {
    pathPrefix("sources") {
      (get & pathPrefix("all")) {
        complete("get")
      } ~ (get & pathPrefix(LongNumber)) { sourceId =>
        complete(s"show $sourceId")
      } ~ (post & pathEndOrSingleSlash) {
        complete("create")
      } ~ (delete & pathPrefix(LongNumber)) { sourceId =>
        complete("delete ")
      } ~ (put & pathPrefix(LongNumber)) { sourceId =>
        complete("update")
      } ~ (put & pathPrefix("markAll")) {
        complete("markAll")
      } ~ (put & pathPrefix("mark" / LongNumber)) { sourceId =>
        complete("mark one")
      } ~ (get & pathPrefix("unread" / LongNumber)) { sourceId =>
        complete("unread")
      } ~ (get & pathPrefix("latest" / LongNumber)) { count =>
        complete("latest")
      } ~ (get & pathPrefix("feeds" / LongNumber)) { sourceId =>
        complete("feeds")
      } ~ (put & pathPrefix("refresh" / LongNumber)) { sourceId =>
        complete("refresh")
      } ~ (put & pathPrefix("refresh")) {
        complete("refresh all")
      } ~ (post & pathPrefix("import")) {
        complete("import")
      } ~ (get & pathPrefix("opml")) {
        complete("opml")
      }
    }
  }

  /*

            get0[SourceController](("unread" / LongNumber) ~> "unread") ~
            get0[SourceController](("latest"  / LongNumber) ~> "latest") ~
            get0[SourceController](("feeds" / LongNumber) ~> "feeds") ~
            put0[SourceController](("refresh" / LongNumber) ~> "refreshOne") ~
            put0[SourceController]("refresh") ~
            post0[SourceController]("import" ~> "fromFile") ~
            get0[SourceController]("opml")
   */

}
