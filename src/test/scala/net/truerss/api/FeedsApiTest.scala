package net.truerss.api

import akka.http.scaladsl.model.StatusCodes
import net.truerss.Gen
import truerss.api._
import truerss.dto.FeedContent
import truerss.util.syntax
import truerss.services.management.FeedsManagement


class FeedsApiTest extends BaseApiTest {

  import syntax.future._
  import JsonFormats._

  private val dto = Gen.genFeedDto
  private val id_200 = 1L
  private val id_404 = 10L
  private val id_500 = 100L
  private val cnt = FeedContent(Some("test"))
  private val fm = mock[FeedsManagement]
  fm.favorites(anyInt, anyInt) returns FeedsResponse(Vector(dto)).toF
  fm.getFeed(id_200) returns FeedResponse(dto).toF
  fm.getFeedContent(id_200) returns FeedContentResponse(cnt).toF
  fm.getFeed(id_404) returns ResponseHelpers.feedNotFound.toF
  fm.getFeed(id_500) returns InternalServerErrorResponse("boom").toF
  fm.changeFavorites(id_200, true) returns FeedResponse(dto).toF
  fm.changeFavorites(id_404, true) returns ResponseHelpers.feedNotFound.toF
  fm.changeFavorites(id_200, false) returns FeedResponse(dto).toF
  fm.changeFavorites(id_404, false) returns ResponseHelpers.feedNotFound.toF
  fm.changeRead(id_200, true) returns FeedResponse(dto).toF
  fm.changeRead(id_404, true) returns ResponseHelpers.feedNotFound.toF
  fm.changeRead(id_200, false) returns FeedResponse(dto).toF
  fm.changeRead(id_404, false) returns ResponseHelpers.feedNotFound.toF

  protected override val r = new FeedsApi(fm).route

  private val url = "/api/v1/feeds"

  "feeds api" should {
    "get favorites" in {
      checkR(Get(s"$url/favorites"), Vector(dto), StatusCodes.OK)
      there was one(fm).favorites(0, 100)

      checkR(Get(s"$url/favorites?offset=10&limit=10"), Vector(dto), StatusCodes.OK)
      there was one(fm).favorites(10, 10)

      checkR(Get(s"$url/favorites?limit=10"), Vector(dto), StatusCodes.OK)
      there was one(fm).favorites(0, 10)

      checkR(Get(s"$url/favorites?offset=10"), Vector(dto), StatusCodes.OK)
      there was one(fm).favorites(0, 10)
    }
    "get feed#ok" in {
      checkR(Get(s"$url/$id_200"), dto)
      there was one(fm).getFeed(id_200)
    }

    "get feed content" in {
      checkR(Get(s"$url/content/$id_200"), cnt)
      there was one(fm).getFeedContent(id_200)
    }

    "get feed#404" in {
      checkR(Get(s"$url/$id_404"), StatusCodes.NotFound)
      there was one(fm).getFeed(id_404)
    }

    "get feed#500" in {
      checkR(Get(s"$url/$id_500"), StatusCodes.InternalServerError)
      there was one(fm).getFeed(id_500)
    }

    "mark#ok" in {
      checkR(Put(s"$url/mark/$id_200"), dto)
      there was one(fm).changeFavorites(id_200, true)
    }

    "mark#404" in {
      checkR(Put(s"$url/mark/$id_404"), StatusCodes.NotFound)
      there was one(fm).changeFavorites(id_404, true)
    }

    "unmark#ok" in {
      checkR(Put(s"$url/unmark/$id_200"), dto)
      there was one(fm).changeFavorites(id_200, false)
    }

    "unmark#404" in {
      checkR(Put(s"$url/unmark/$id_404"), StatusCodes.NotFound)
      there was one(fm).changeFavorites(id_404, false)
    }

    "read#ok" in {
      checkR(Put(s"$url/read/$id_200"), dto)
      there was one(fm).changeRead(id_200, true)
    }

    "read#404" in {
      checkR(Put(s"$url/read/$id_404"), StatusCodes.NotFound)
      there was one(fm).changeRead(id_404, true)
    }

    "unread#ok" in {
      checkR(Put(s"$url/unread/$id_200"), dto)
      there was one(fm).changeRead(id_200, false)
    }

    "unread#404" in {
      checkR(Put(s"$url/unread/$id_404"), StatusCodes.NotFound)
      there was one(fm).changeRead(id_404, false)
    }
  }



}
