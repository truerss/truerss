package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.services.management.FeedsManagement
import truerss.util.Util

import scala.concurrent.ExecutionContext

class FeedsApi(val feedsManagement: FeedsManagement)
              (
                implicit val ec: ExecutionContext
              ) extends HttpHelper {

  import Util.StringExt
  import ApiImplicits._

  private val fm = feedsManagement

  val route = api {
    pathPrefix("feeds") {
      get {
        pathPrefix("favorites") {
          parameters('offset ? "0", 'limit ? "100") { (offset, limit) =>
            fm.favorites(offset.toIntOr(0), limit.toIntOr(100))
          }
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
