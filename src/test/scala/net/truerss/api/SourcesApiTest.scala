package net.truerss.api

import java.io.File

import akka.http.scaladsl.model.{ContentTypes, Multipart, StatusCodes}
import akka.http.scaladsl.server.Route
import net.truerss.Gen
import play.api.libs.json._
import truerss.api._
import truerss.dto.{FeedDto, Page, SourceOverview}
import truerss.util.syntax
import truerss.services.management._

class SourcesApiTest extends BaseApiTest {

  sequential

  import syntax.future._
  import FeedSourceDtoModelImplicits._
  import JsonFormats._

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
  private val offset = 0
  private val limit = 100

  private val fm = mock[FeedsManagement]
  private val om = mock[OpmlManagement]
  private val sm = mock[SourcesManagement]
  sm.all returns SourcesResponse(Vector(dto)).toF
  sm.getSource(id_200) returns SourceResponse(dto).toF
  sm.getSource(id_404) returns ResponseHelpers.sourceNotFound.toF
  sm.addSource(newDto) returns SourceResponse(dto).toF
  sm.addSource(notValidNewDto) returns BadRequestResponse("boom").toF
  sm.deleteSource(id_200) returns ResponseHelpers.ok.toF
  sm.deleteSource(id_404) returns ResponseHelpers.sourceNotFound.toF
  sm.updateSource(validUpdateDto.id, validUpdateDto) returns SourceResponse(dto).toF
  sm.updateSource(notValidUpdateDto.id, notValidUpdateDto) returns BadRequestResponse("boom").toF
  sm.markSource(id_200) returns ResponseHelpers.ok.toF
  sm.forceRefreshSource(sId1) returns ResponseHelpers.ok.toF
  sm.forceRefresh returns ResponseHelpers.ok.toF
  sm.getSourceOverview(id_200) returns SourceOverViewResponse(SourceOverview.empty(id_200)).toF

  fm.markAll returns ResponseHelpers.ok.toF
  fm.findUnreadBySource(id_200) returns FeedsResponse(Vector(unreadFeed)).toF
  fm.latest(anyInt, anyInt) returns FeedsResponse(Vector(unreadFeed)).toF
  fm.fetchBySource(anyLong, anyBoolean, anyInt, anyInt) returns FeedsPageResponse(Page.empty[FeedDto]).toF

  om.getOpml returns Ok.toF
  om.createFrom(opml) returns ImportResponse(Vector.empty).toF

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

    "get overview" in {
      checkR(Get(s"$url/overview/$id_200"), StatusCodes.OK)
      there was one(sm).getSourceOverview(id_200)
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
      checkR(Delete(s"$url/$id_200"), StatusCodes.NoContent)
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
      checkR(Put(s"$url/markall"), StatusCodes.NoContent)
      there was one(fm).markAll
    }

    "mark one" in {
      checkR(Put(s"$url/mark/$id_200"), StatusCodes.NoContent)
      there was one(sm).markSource(id_200)
    }

    "get unread by source" in {
      checkR(Get(s"$url/unread/$id_200"), Vector(unreadFeed))
      there was one(fm).findUnreadBySource(id_200)
    }

    "latest" in {
      checkR(Get(s"$url/latest?offset=0&limit=100"), Vector(unreadFeed))
      there was one(fm).latest(0, 100)

      checkR(Get(s"$url/latest?offset=10"), Vector(unreadFeed))
      there was one(fm).latest(10, 100)

      checkR(Get(s"$url/latest?limit=10"), Vector(unreadFeed))
      there was one(fm).latest(0, 100)
    }

    "fetch by source" should {
      "pass default parameters" in {
        checkR(Get(s"$url$sId1/feeds"), Page.empty[FeedDto])

        there was one(fm).fetchBySource(sId1, true, 0, 100)
      }

      "pass limit parameter" in {
        checkR(Get(s"$url$sId1/feeds?limit=123"), Page.empty[FeedDto])

        there was one(fm).fetchBySource(sId1, true, 0, 123)
      }

      "pass offset parameter" in {
        checkR(Get(s"$url$sId1/feeds?offset=10"), Page.empty[FeedDto])

        there was one(fm).fetchBySource(sId1, true, 10, 100)
      }

      "pass unreadOnly parameter" in {
        checkR(Get(s"$url$sId1/feeds?unreadOnly=false"), Page.empty[FeedDto])

        there was one(fm).fetchBySource(sId1, false, 0, 100)
      }
    }

    "refresh all" in {
      checkR(Put(s"$url/refresh"), StatusCodes.NoContent)
      there was one(sm).forceRefresh
    }

    "refresh single" in {
      checkR(Put(s"$url/refresh/$sId1"), StatusCodes.NoContent)
      there was one(sm).forceRefreshSource(sId1)
    }

    "get opml" in {
      checkR(Get(s"$url/opml"), StatusCodes.NoContent)
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
