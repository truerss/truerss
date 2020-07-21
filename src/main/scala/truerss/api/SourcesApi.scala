package truerss.api

import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import truerss.dto.{FeedDto, NewSourceDto, Page, SourceOverview, SourceViewDto, UpdateSourceDto}
import truerss.services.{FeedsService, SourceOverviewService, SourcesService}
import truerss.services.management.{FeedsManagement, OpmlManagement, SourcesManagement}
import truerss.util.Util

import scala.concurrent.ExecutionContext

class SourcesApi(sourcesManagement: SourcesManagement,
                 feedsManagement: FeedsManagement,
                 opmlManagement: OpmlManagement,
                 feedsService: FeedsService,
                 sourcesService: SourcesService,
                 sourceOverviewService: SourceOverviewService
                )(
  implicit val ec: ExecutionContext,
  val materializer: Materializer
) extends HttpHelper {

  import JsonFormats._
  import Util.StringExt
  import ApiImplicits._

  // just aliases
  private val sm = sourcesManagement
  private val fm = feedsManagement
  private val om = opmlManagement
  private val fs = feedsService
  private val ss = sourcesService
  private val sos = sourceOverviewService

  val route = api {
    pathPrefix("sources") {
      get {
        pathPrefix("all") {
          w[Vector[SourceViewDto]](ss.getAll)
        } ~ (pathPrefix(LongNumber) & pathEnd) { sourceId =>
          w[SourceViewDto](ss.getSource(sourceId))
        } ~ pathPrefix("overview"/ LongNumber) { sourceId =>
          w[SourceOverview](sos.getSourceOverview(sourceId))
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
        } ~ pathPrefix("opml") {
          om.getOpml
        }
      } ~ post {
        pathPrefix("import") {
          makeImport
        } ~ pathEndOrSingleSlash {
          createT[NewSourceDto](x => sm.addSource(x))
        }
      } ~ put {
        pathPrefix(LongNumber) { sourceId =>
          createT[UpdateSourceDto](x => sm.updateSource(sourceId, x))
        } ~ pathPrefix("markall") {
          w[Unit](fs.markAllAsRead)
        } ~ pathPrefix("mark" / LongNumber) { sourceId =>
          w[Unit](ss.markAsRead(sourceId))
        } ~ pathPrefix("refresh" / LongNumber) { sourceId =>
          w[Unit](ss.refreshSource(sourceId))
        } ~ pathPrefix("refresh") {
          w[Unit](ss.refreshAll)
        }
      } ~ delete {
        pathPrefix(LongNumber) { sourceId =>
          w[Unit](ss.delete(sourceId))
        }
      }
    }
  }

  // custom thread !!! TODO
  protected def makeImport: Route = {
    entity(as[Multipart.FormData]) { formData =>
      val r = formData.parts.mapAsync(1) { p =>
        p.entity.dataBytes.runFold("") { (a, b) =>
          a + b.decodeString(utf8)
        }
      }.runFold("") { _ + _ }.flatMap { x =>
        zio.Runtime.default.unsafeRunToFuture(om.createFrom(x))
      }

      call(r)
    }
  }

}