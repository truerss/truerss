package net.truerss.services
import java.io.File

import akka.http.scaladsl.model.{ContentTypes, Multipart, StatusCodes}
import akka.http.scaladsl.server.Route
import truerss.api.{BadRequestResponse, FeedsResponse, ImportResponse, JsonFormats, Ok, SourceResponse, SourcesApi, SourcesResponse}
import truerss.services.management.{FeedsManagement, OpmlManagement, SourcesManagement}
import truerss.services.management.DtoModelImplicits
import play.api.libs.json._
import truerss.dto.FeedDto
import truerss.util.Util.ResponseHelpers

class SourcesApiTest extends BaseApiTest {

  import JsonFormats._
  import DtoModelImplicits._

  private val uri = getClass.getResource("/1.txt").toURI

  private val id_200 = 1L
  private val id_404 = 10L
  private val sId1 = 100L
  private val opml = scala.io.Source.fromFile(uri).mkString
  private val dto = Gen.genSource(Some(id_200)).toView
  private val newDto = Gen.genNewSource
  private val notValidNewDto = Gen.genNewSource
  private val validUpdateDto = Gen.genUpdSource(id_200)
  private val notValidUpdateDto = Gen.genUpdSource(id_404)
  private val unreadFeed = Gen.genFeedDto
  private val count = 1000

  private val fm = mock[FeedsManagement]
  private val om = mock[OpmlManagement]
  private val sm = mock[SourcesManagement]
  sm.all returns f(SourcesResponse(Vector(dto)))
  sm.getSource(id_200) returns f(SourceResponse(dto))
  sm.getSource(id_404) returns f(ResponseHelpers.sourceNotFound)
  sm.addSource(newDto) returns f(SourceResponse(dto))
  sm.addSource(notValidNewDto) returns f(BadRequestResponse("boom"))
  sm.deleteSource(id_200) returns f(ResponseHelpers.ok)
  sm.deleteSource(id_404) returns f(ResponseHelpers.sourceNotFound)
  sm.updateSource(validUpdateDto.id, validUpdateDto) returns f(SourceResponse(dto))
  sm.updateSource(notValidUpdateDto.id, notValidUpdateDto) returns f(BadRequestResponse("boom"))
  sm.markSource(id_200) returns f(ResponseHelpers.ok)
  sm.forceRefreshSource(sId1) returns f(ResponseHelpers.ok)
  sm.forceRefresh returns f(ResponseHelpers.ok)

  fm.markAll returns f(ResponseHelpers.ok)
  fm.findUnreadBySource(id_200) returns f(FeedsResponse(Vector(unreadFeed)))
  fm.latest(count) returns f(FeedsResponse(Vector(unreadFeed)))
  fm.fetchBySource(anyLong, anyInt, anyInt) returns f(FeedsResponse(Vector()))

  om.getOpml returns f(Ok(opml))
  om.createFrom(opml) returns f(ImportResponse(Map.empty))

  override protected val r: Route = new SourcesApi(sm, fm, om).route

  private val url = "/api/v1/sources"

  "sources api" should {
    "get all" in {
      checkR(Get(s"$url/all"), Vector(dto))
      there was one(sm).all
    }

    "get source#ok" in {
      checkR(Get(s"$url/$id_200"), dto)
      there was one(sm).getSource(id_200)
    }

    "get source#404" in {
      checkR(Get(s"$url/$id_404"), nf)
      there was one(sm).getSource(id_404)
    }

    "create new source#ok" in {
      val source = Json.toJson(newDto).toString()
      checkR(Post(s"$url", source), dto)
      there was one(sm).addSource(newDto)
    }

    "create new source#400" in {
      val source = Json.toJson(notValidNewDto).toString()
      checkR(Post(s"$url", source), dto, StatusCodes.BadRequest)
      there was one(sm).addSource(notValidNewDto)
    }

    "delete source#ok" in {
      checkR(Delete(s"$url/$id_200"), StatusCodes.OK)
    }

    "delete source#404" in {
      checkR(Delete(s"$url/$id_404"), StatusCodes.NotFound)
    }

    "update#ok" in {
      val source = Json.toJson(validUpdateDto).toString()
      checkR(Put(s"$url/${validUpdateDto.id}", source), dto)
      there was one(sm).updateSource(validUpdateDto.id, validUpdateDto)
    }

    "update#400" in {
      val source = Json.toJson(notValidUpdateDto).toString()
      checkR(Put(s"$url/${notValidUpdateDto.id}", source), dto, StatusCodes.BadRequest)
      there was one(sm).updateSource(notValidUpdateDto.id, notValidUpdateDto)
    }

    "markall" in {
      checkR(Put(s"$url/markall"), StatusCodes.OK)
      there was one(fm).markAll
    }

    "mark one" in {
      checkR(Put(s"$url/mark/$id_200"), StatusCodes.OK)
      there was one(sm).markSource(id_200)
    }

    "get unread by source" in {
      checkR(Get(s"$url/unread/$id_200"), Vector(unreadFeed))
      there was one(fm).findUnreadBySource(id_200)
    }

    "latest" in {
      checkR(Get(s"$url/latest/$count"), Vector(unreadFeed))
      there was one(fm).latest(count)
    }

    "fetch by source" in {
      checkR(Get(s"$url/feeds/$sId1"), Vector.empty[FeedDto])

      there was one(fm).fetchBySource(anyLong, anyInt, anyInt)
    }

    "refresh all" in {
      checkR(Put(s"$url/refresh"), StatusCodes.OK)
      there was one(sm).forceRefresh
    }

    "refresh single" in {
      checkR(Put(s"$url/refresh/$sId1"), StatusCodes.OK)
      there was one(sm).forceRefreshSource(sId1)
    }

    "get opml" in {
      checkR(Get(s"$url/opml"), StatusCodes.OK)
      there was one(om).getOpml
    }

    "import opml" in {
      val formData = Multipart.FormData(
        Multipart.FormData.BodyPart.fromPath(
          "foo.opml",
          contentType = ContentTypes.`application/octet-stream`,
          file = new File(uri).toPath
        )
      )
      checkR(Post(s"$url/import", formData), StatusCodes.OK)
      there was one(om).createFrom(opml)
    }
  }
}
