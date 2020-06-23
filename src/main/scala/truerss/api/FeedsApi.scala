package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.services.management.FeedsManagement

import scala.concurrent.ExecutionContext

class FeedsApi(val feedsManagement: FeedsManagement)
              (
                implicit val ec: ExecutionContext
              ) extends HttpHelper {

  import ApiImplicits._

  private val fm = feedsManagement

  val route = api {
    pathPrefix("feeds") {
      get {
        pathPrefix("favorites") {
          fm.favorites
        } ~ pathPrefix(LongNumber) { feedId =>
          fm.getFeed(feedId)
        } ~ pathPrefix("content" / LongNumber) { feedId =>
          fm.getFeedContent(feedId)
        }
      } ~ put {
        pathPrefix("mark" / LongNumber) { feedId =>
          fm.addToFavorites(feedId)
        } ~ pathPrefix("unmark" / LongNumber) { feedId =>
          fm.removeFromFavorites(feedId)
        } ~ pathPrefix("read" / LongNumber) { feedId =>
          fm.markAsRead(feedId)
        } ~ pathPrefix("unread" / LongNumber) { feedId =>
          fm.markAsUnread(feedId)
        }
      }
    }
  }

}
