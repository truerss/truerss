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
    } ~ pathPrefix("feeds") {
      (get & pathPrefix("favorites")) {
        complete("favorites")
      } ~ (get & pathPrefix(LongNumber)) { feedId =>
        complete("get one feed")
      } ~ put {
        pathPrefix("mark" / LongNumber) { feedId =>
          complete("mark feed as fav")
        } ~ pathPrefix("unmark" / LongNumber) { feedId =>
          complete("mark feed as unfav")
        } ~ pathPrefix("read" / LongNumber) { feedId =>
          complete("mark as read feed")
        } ~ pathPrefix("unread" / LongNumber) { feedId =>
          complete("mark as unread")
        }
      }
    } ~ pathPrefix("plugins") {
      get {
        pathPrefix("all") {
          complete("all")
        } ~ pathPrefix("js") {
          complete("js")
        } ~ pathPrefix("css") {
          complete("css")
        }
      }
    } ~ pathPrefix("system") {
      get {
        pathPrefix("stop") {
          complete("stop")
        } ~ pathPrefix("restart") {
          complete("restart")
        } ~ pathPrefix("exit") {
          complete("exit")
        }
      }
    }
  }

  /*
  get0[SystemController]("stop") ~
            get0[SystemController]("restart") ~
            get0[SystemController]("exit")
            */


}
