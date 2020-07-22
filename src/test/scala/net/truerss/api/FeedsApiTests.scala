package net.truerss.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import net.truerss.Gen
import truerss.api.FeedsApi
import truerss.services._
import truerss.api.JsonFormats
import play.api.libs.json.Json
import truerss.db.DbLayer
import truerss.dto.{FeedContent, FeedDto, Page}
import zio.Task

class FeedsApiTests extends BaseApiTest {

  import JsonFormats._

  private val path = "feeds"
  private val content = FeedContent(Some("content"))
  private val error = "boom"
  private val feedId_200 = 1
  private val feedId_404 = 10
  private val feedId_503 = 100
  private val feedDto = Gen.genFeedDto.copy(id = feedId_200)
  private val page = Page[FeedDto](1, feedDto :: Nil)


  "feeds api" should {
    "one" should {
      "#ok" in new Test() {
        Get(api(s"$path/$feedId_200")) ~> route ~> check {
          responseAs[String] ==== Json.toJson(feedDto).toString
          status ==== StatusCodes.OK
        }
      }

      "#notFound" in new Test() {
        Get(api(s"$path/$feedId_404")) ~> route ~> check {
          status ==== StatusCodes.NotFound
        }
      }
    }

    "content" should {
      "#ok" in new Test() {
        Get(api(s"$path/content/$feedId_200")) ~> route ~> check {
          status ==== StatusCodes.OK
        }
      }

      "#notFound" in new Test() {
        Get(api(s"$path/content/$feedId_404")) ~> route ~> check {
          status ==== StatusCodes.NotFound
        }
      }

      "remove server is not available" in new Test() {
        Get(api(s"$path/content/$feedId_503")) ~> route ~> check {
          status ==== StatusCodes.InternalServerError
        }
      }
    }

    "favorites" in {
      "simple filter" in new Test() {
        Get(api(s"$path/favorites")) ~> route ~> check {
          responseAs[String] ==== Json.toJson(page).toString
          status ==== StatusCodes.OK
        }
        feedsService.savedOffset ==== 0
        feedsService.savedLimit ==== 100
      }

      "invalid offset" in new Test() {
        Get(api(s"$path/favorites?offset=asd&limit=10")) ~> route ~> check {
          responseAs[String] ==== Json.toJson(page).toString
          status ==== StatusCodes.OK
        }
        feedsService.savedOffset ==== 0
        feedsService.savedLimit ==== 10
      }

      "invalid limit" in new Test() {
        Get(api(s"$path/favorites?offset=33&limit=qwe")) ~> route ~> check {
          responseAs[String] ==== Json.toJson(page).toString
          status ==== StatusCodes.OK
        }
        feedsService.savedOffset ==== 33
        feedsService.savedLimit ==== 100
      }
    }

    "mark" in new Test() {
      Put(api(s"$path/mark/$feedId_200")) ~> route ~> check {
        status ==== StatusCodes.NoContent
      }
      feedsService.savedFeedId ==== feedId_200
      feedsService.savedFlag ==== true
    }

    "unmark" in new Test() {
      Put(api(s"$path/unmark/$feedId_200")) ~> route ~> check {
        status ==== StatusCodes.NoContent
      }
      feedsService.savedFeedId ==== feedId_200
      feedsService.savedFlag ==== false
    }

    "read" in new Test() {
      Put(api(s"$path/read/$feedId_200")) ~> route ~> check {
        status ==== StatusCodes.NoContent
      }
      feedsService.savedFeedId ==== feedId_200
      feedsService.savedFlag ==== true
    }

    "unread" in new Test() {
      Put(api(s"$path/unread/$feedId_200")) ~> route ~> check {
        status ==== StatusCodes.NoContent
      }
      feedsService.savedFeedId ==== feedId_200
      feedsService.savedFlag ==== false
    }
  }

  class Test extends BaseScope {
    val feedsService = new FeedsService(null) {
      var savedOffset = 0
      var savedLimit = 0
      var savedFeedId = 0L
      var savedFlag = true
      override def findOne(feedId: Long): Task[FeedDto] = {
        if (feedId == feedId_200) {
          Task.succeed(feedDto)
        } else {
          Task.fail(NotFoundError(feedId_404))
        }
      }

      override def favorites(offset: Int, limit: Int): Task[Page[FeedDto]] = {
        savedOffset = offset
        savedLimit = limit
        Task.succeed(page)
      }

      override def changeFav(feedId: Long, favFlag: Boolean): Task[Unit] = {
        savedFeedId = feedId
        savedFlag = favFlag
        Task.succeed(())
      }

      override def changeRead(feedId: Long, readFlag: Boolean): Task[Unit] = {
        savedFeedId = feedId
        savedFlag = readFlag
        Task.succeed(())
      }
    }

    private val contentReaderService = new ContentReaderService(
      null,
      null,
      null
    ) {
      override def fetchFeedContent(feedId: Long): Task[FeedContent] = {
        feedId match {
          case `feedId_200` => Task.succeed(content)
          case `feedId_404` => Task.fail(NotFoundError(feedId_404))
          case _ => Task.fail(ContentReadError(error))
        }
      }
    }

    override protected val route: Route = new FeedsApi(feedsService, contentReaderService).route
  }


}
