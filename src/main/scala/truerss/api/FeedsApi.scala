package truerss.api

import akka.actor.ActorRef
import akka.stream.Materializer
import akka.http.scaladsl.server.Directives._
import truerss.services.management.FeedsManagement

import scala.concurrent.ExecutionContext

class FeedsApi(val feedsManagement: FeedsManagement)
              (
                implicit override val ec: ExecutionContext,
                val materializer: Materializer
              ) extends HttpHelper {
  override val service: ActorRef = null

  val fm = feedsManagement

  val route = api {
    pathPrefix("feeds") {
      (get & pathPrefix("favorites")) {
        call(fm.favorites)
      } ~ (get & pathPrefix(LongNumber)) { feedId =>
        call(fm.getFeed(feedId))
      } ~ put {
        pathPrefix("mark" / LongNumber) { feedId =>
          call(fm.addToFavorites(feedId))
        } ~ pathPrefix("unmark" / LongNumber) { feedId =>
          call(fm.removeFromFavorites(feedId))
        } ~ pathPrefix("read" / LongNumber) { feedId =>
          call(fm.markAsRead(feedId))
        } ~ pathPrefix("unread" / LongNumber) { feedId =>
          call(fm.markAsUnread(feedId))
        }
      }
    }
  }

}
