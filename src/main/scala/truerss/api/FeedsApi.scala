package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.dto.{FeedDto, Page}
import truerss.services.FeedsService
import truerss.services.management.FeedsManagement
import truerss.util.Util

import scala.concurrent.ExecutionContext

class FeedsApi(val feedsManagement: FeedsManagement,
               feedsService: FeedsService)
              (
                implicit val ec: ExecutionContext
              ) extends HttpHelper {

  import Util.StringExt
  import ApiImplicits._
  import JsonFormats._

  private val fm = feedsManagement
  private val fs = feedsService

  val route = api {
    pathPrefix("feeds") {
      get {
        pathPrefix("favorites") {
          parameters('offset ? "0", 'limit ? "100") { (offset, limit) =>
            w[Page[FeedDto]](fs.favorites(offset.toIntOr(0), limit.toIntOr(100)))
          }
        } ~ pathPrefix(LongNumber) { feedId =>
          w[FeedDto](fs.findOne(feedId))
        } ~ pathPrefix("content" / LongNumber) { feedId =>
          fm.getFeedContent(feedId)
        }
      } ~ put {
        pathPrefix("mark" / LongNumber) { feedId =>
          w[FeedDto](fs.changeFav(feedId, favFlag = true))
        } ~ pathPrefix("unmark" / LongNumber) { feedId =>
          w[FeedDto](fs.changeFav(feedId, favFlag = false))
        } ~ pathPrefix("read" / LongNumber) { feedId =>
          w[FeedDto](fs.changeRead(feedId, readFlag = true))
        } ~ pathPrefix("unread" / LongNumber) { feedId =>
          w[FeedDto](fs.changeRead(feedId, readFlag = false))
        }
      }
    }
  }

}
