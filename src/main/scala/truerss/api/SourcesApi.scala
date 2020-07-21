package truerss.api

import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import truerss.dto.{FeedDto, NewSourceDto, Page, SourceOverview, SourceViewDto, UpdateSourceDto}
import truerss.services.{FeedsService, OpmlService, SourceOverviewService, SourcesService}
import truerss.util.Util
import zio.Task

import scala.concurrent.ExecutionContext

class SourcesApi(feedsService: FeedsService,
                 sourcesService: SourcesService,
                 sourceOverviewService: SourceOverviewService,
                 opmlService: OpmlService
                )(
  implicit val ec: ExecutionContext,
  val materializer: Materializer
) extends HttpApi {

  import JsonFormats._
  import Util.StringExt

  // just aliases
  private val fs = feedsService
  private val ss = sourcesService
  private val sos = sourceOverviewService
  private val os = opmlService

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
          // todo custom header probably
          w[String](os.build)
        }
      } ~ post {
        pathPrefix("import") {
          makeImport
        } ~ pathEndOrSingleSlash {
          createTR[NewSourceDto, SourceViewDto](ss.addSource)
        }
      } ~ put {
        pathPrefix(LongNumber) { sourceId =>
          createTR[UpdateSourceDto, SourceViewDto](x => ss.updateSource(sourceId, x))
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
        zio.Runtime.default.unsafeRunToFuture(os.create(x))
      }

      taskCall1[Iterable[SourceViewDto]](Task.fromFuture( implicit ec => r))
    }
  }

}