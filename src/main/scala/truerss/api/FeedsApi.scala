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
          fm.changeFavorites(feedId, favFlag = true)
        } ~ pathPrefix("unmark" / LongNumber) { feedId =>
          fm.changeFavorites(feedId, favFlag = false)
        } ~ pathPrefix("read" / LongNumber) { feedId =>
          fm.changeRead(feedId, readFlag = true)
        } ~ pathPrefix("unread" / LongNumber) { feedId =>
          fm.changeRead(feedId, readFlag = false)
        }
      }
    }
  }

}
