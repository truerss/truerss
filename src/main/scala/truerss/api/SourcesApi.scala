package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.dto._
import truerss.services.{FeedsService, SourcesService}
import truerss.util.Util

class SourcesApi(feedsService: FeedsService,
                 sourcesService: SourcesService
                ) extends HttpApi {

  import JsonFormats._
  import Util.StringExt

  // just aliases
  private val fs = feedsService
  private val ss = sourcesService

  val route = api {
    pathPrefix("sources") {
      get {
        pathPrefix("all") {
          w[Vector[SourceViewDto]](ss.getAll)
        } ~ (pathPrefix(LongNumber) & pathEnd) { sourceId =>
          w[SourceViewDto](ss.getSource(sourceId))
        } ~ pathPrefix("latest") {
          parameters('offset ? "0", 'limit ? "100") { (from, to) =>
            w[Page[FeedDto]](fs.latest(from.toIntOr(0), to.toIntOr(100)))
          }
        } ~ pathPrefix(LongNumber / "feeds") { sourceId =>
          parameters('unreadOnly ? true, 'offset ? "0", 'limit ? "100") { (unreadOnly, from, limit) =>
            w[Page[FeedDto]](fs.findBySource(sourceId, unreadOnly, from.toIntOr(0), limit.toIntOr(100)))
          }
        } ~ pathPrefix("unread" / LongNumber) { sourceId =>
          w[Vector[FeedDto]](fs.findUnread(sourceId))
        }
      } ~ post {
        pathEndOrSingleSlash {
          createTR[NewSourceDto, SourceViewDto](ss.addSource)
        }
      } ~ put {
        pathPrefix(LongNumber) { sourceId =>
          createTR[UpdateSourceDto, SourceViewDto](x => ss.updateSource(sourceId, x))
        }
      } ~ delete {
        pathPrefix(LongNumber) { sourceId =>
          w[Unit](ss.delete(sourceId))
        }
      }
    }
  }

}