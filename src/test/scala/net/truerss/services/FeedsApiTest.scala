package net.truerss.services

import akka.http.scaladsl.model.{HttpRequest, StatusCode, StatusCodes}
import akka.http.scaladsl.testkit.{RouteTest, Specs2FrameworkInterface}
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.AllExpectations
import play.api.libs.json._
import truerss.api._
import truerss.services.management.FeedsManagement
import truerss.util.Util.ResponseHelpers

import scala.concurrent.Future


class FeedsApiTest extends BaseApiTest {

  import JsonFormats._

  private val dto = Gen.genFeedDto
  private val id_200 = 1L
  private val id_404 = 10L
  private val id_500 = 100L
  private val fm = mock[FeedsManagement]
  fm.favorites returns f(FeedsResponse(Vector(dto)))
  fm.getFeed(id_200) returns f(FeedResponse(dto))
  fm.getFeed(id_404) returns f(ResponseHelpers.feedNotFound)
  fm.getFeed(id_500) returns f(InternalServerErrorResponse("boom"))
  fm.addToFavorites(id_200) returns f(FeedResponse(dto))
  fm.addToFavorites(id_404) returns f(ResponseHelpers.feedNotFound)
  fm.removeFromFavorites(id_200) returns f(FeedResponse(dto))
  fm.removeFromFavorites(id_404) returns f(ResponseHelpers.feedNotFound)
  fm.markAsRead(id_200) returns f(FeedResponse(dto))
  fm.markAsRead(id_404) returns f(ResponseHelpers.feedNotFound)
  fm.markAsUnread(id_200) returns f(FeedResponse(dto))
  fm.markAsUnread(id_404) returns f(ResponseHelpers.feedNotFound)

  protected override val r = new FeedsApi(fm).route

  private val url = "/api/v1/feeds"

  "feeds api" should {
    "get favorites" in {
      checkR(Get(s"$url/favorites"), Vector(dto), StatusCodes.OK)
      there was one(fm).favorites
    }
    "get feed#ok" in {
      checkR(Get(s"$url/$id_200"), dto)
      there was one(fm).getFeed(id_200)
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
      there was one(fm).addToFavorites(id_200)
    }

    "mark#404" in {
      checkR(Put(s"$url/mark/$id_404"), StatusCodes.NotFound)
      there was one(fm).addToFavorites(id_404)
    }

    "unmark#ok" in {
      checkR(Put(s"$url/unmark/$id_200"), dto)
      there was one(fm).removeFromFavorites(id_200)
    }

    "unmark#404" in {
      checkR(Put(s"$url/unmark/$id_404"), StatusCodes.NotFound)
      there was one(fm).removeFromFavorites(id_404)
    }

    "read#ok" in {
      checkR(Put(s"$url/read/$id_200"), dto)
      there was one(fm).markAsRead(id_200)
    }

    "read#404" in {
      checkR(Put(s"$url/read/$id_404"), StatusCodes.NotFound)
      there was one(fm).markAsRead(id_404)
    }

    "unread#ok" in {
      checkR(Put(s"$url/unread/$id_200"), dto)
      there was one(fm).markAsUnread(id_200)
    }

    "unread#404" in {
      checkR(Put(s"$url/unread/$id_404"), StatusCodes.NotFound)
      there was one(fm).markAsUnread(id_404)
    }
  }



}
