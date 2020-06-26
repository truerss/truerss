package truerss.api

import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import truerss.dto.{NewSourceDto, UpdateSourceDto}
import truerss.services.management.{FeedsManagement, OpmlManagement, SourcesManagement}
import truerss.util.Util

import scala.concurrent.{ExecutionContext, Future}

class SourcesApi(sourcesManagement: SourcesManagement,
                 feedsManagement: FeedsManagement,
                 opmlManagement: OpmlManagement,
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

  val route = api {
    pathPrefix("sources") {
      get {
        pathPrefix("all") {
          sm.all
        } ~ (pathPrefix(LongNumber) & pathEnd) { sourceId =>
          sm.getSource(sourceId)
        } ~ pathPrefix("overview"/ LongNumber) { sourceId =>
          sm.getSourceOverview(sourceId)
        } ~ pathPrefix("latest") {
          parameters('offset ? "0", 'limit ? "100") { (from, to) =>
            fm.latest(from.toIntOr(0), to.toIntOr(100))
          }
        } ~ pathPrefix(LongNumber / "feeds") { sourceId =>
          parameters('unreadOnly ? true, 'offset ? "0", 'limit ? "100") { (unreadOnly, from, limit) =>
            fm.fetchBySource(sourceId, unreadOnly, from.toIntOr(0), limit.toIntOr(100))
          }
        } ~ pathPrefix("unread" / LongNumber) { sourceId =>
          fm.findUnreadBySource(sourceId)
        } ~ pathPrefix("opml") {
          om.getOpml
        }
      } ~ post {
        pathPrefix("import") {
          makeImport
        } ~ pathEndOrSingleSlash {
          create[NewSourceDto](x => sm.addSource(x))
        }
      } ~ put {
        pathPrefix(LongNumber) { sourceId =>
          create[UpdateSourceDto](x => sm.updateSource(sourceId, x))
        } ~ pathPrefix("markall") {
          fm.markAll
        } ~ pathPrefix("mark" / LongNumber) { sourceId =>
          sm.markSource(sourceId)
        } ~ pathPrefix("refresh" / LongNumber) { sourceId =>
          sm.forceRefreshSource(sourceId)
        } ~ pathPrefix("refresh") {
          sm.forceRefresh
        }
      } ~ delete {
        pathPrefix(LongNumber) { sourceId =>
          sm.deleteSource(sourceId)
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
        om.createFrom(x)
      }

      call(r)
    }
  }

}