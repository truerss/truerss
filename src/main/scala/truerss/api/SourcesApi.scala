package truerss.api

import akka.http.scaladsl.model.Multipart
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import truerss.dto.{NewSourceDto, UpdateSourceDto}
import truerss.services.management.{FeedsManagement, OpmlManagement, SourcesManagement}
import truerss.util.Util

import scala.concurrent.ExecutionContext


class SourcesApi(sourcesManagement: SourcesManagement,
                 feedsManagement: FeedsManagement,
                 opmlManagement: OpmlManagement,
                )(
  implicit override val ec: ExecutionContext,
  val materializer: Materializer
) extends HttpHelper {

  import JsonFormats._
  import Util.StringExt

  val sm = sourcesManagement
  val fm = feedsManagement
  val om = opmlManagement

  val route = api {
    pathPrefix("sources") {
      (get & pathPrefix("all")) {
        call(sm.all)
      } ~ (get & pathPrefix(LongNumber)) { sourceId =>
        call(sm.getSource(sourceId))
      } ~ (get & pathPrefix("overview"/ LongNumber)) { sourceId =>
        call(sm.getSourceOverview(sourceId))
      } ~ (post & pathEndOrSingleSlash) {
        create[NewSourceDto](x => sm.addSource(x))
      } ~ (delete & pathPrefix(LongNumber)) { sourceId =>
        call(sm.deleteSource(sourceId))
      } ~ (put & pathPrefix(LongNumber)) { sourceId =>
        create[UpdateSourceDto](x => sm.updateSource(sourceId, x))
      } ~ (put & pathPrefix("markall")) {
        call(fm.markAll)
      } ~ (put & pathPrefix("mark" / LongNumber)) { sourceId =>
        call(sm.markSource(sourceId))
      } ~ (get & pathPrefix("unread" / LongNumber)) { sourceId =>
        call(fm.findUnreadBySource(sourceId))
      } ~ (get & pathPrefix("latest" / IntNumber)) { count =>
        call(fm.latest(count))
      } ~ (get & pathPrefix(LongNumber / "feeds")) { sourceId =>
        parameters('unreadOnly ? true, 'offset ? "0", 'limit ? "100") { (unreadOnly, from, limit) =>
          call(fm.fetchBySource(sourceId, unreadOnly, from.toIntOr(0), limit.toIntOr(0)))
        }
      } ~ (put & pathPrefix("refresh" / LongNumber)) { sourceId =>
        call(sm.forceRefreshSource(sourceId))
      } ~ (put & pathPrefix("refresh")) {
        call(sm.forceRefresh)
      } ~ (post & pathPrefix("import")) {
        makeImport
      } ~ (get & pathPrefix("opml")) {
        call(om.getOpml)
      }
    }
  }

  // custom thread !!! TODO
  def makeImport = {
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
