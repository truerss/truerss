package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.dto.{FeedContent, FeedDto, Page}
import truerss.services.{ContentReaderService, FeedsService}
import truerss.util.Util

class FeedsApi(feedsService: FeedsService,
               contentReaderService: ContentReaderService
              ) extends HttpApi {

  import Util.StringExt
  import JsonFormats._

  private val fs = feedsService
  private val crs = contentReaderService

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
          w[FeedContent](crs.fetchFeedContent(feedId))
        }
      } ~ put {
        pathPrefix("mark" / LongNumber) { feedId =>
          w[Unit](fs.changeFav(feedId, favFlag = true))
        } ~ pathPrefix("unmark" / LongNumber) { feedId =>
          w[Unit](fs.changeFav(feedId, favFlag = false))
        } ~ pathPrefix("read" / LongNumber) { feedId =>
          w[Unit](fs.changeRead(feedId, readFlag = true))
        } ~ pathPrefix("unread" / LongNumber) { feedId =>
          w[Unit](fs.changeRead(feedId, readFlag = false))
        }
      }
    }
  }

}
