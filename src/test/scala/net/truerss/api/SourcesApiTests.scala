package net.truerss.api

import akka.event.EventStream
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import net.truerss.Gen
import play.api.libs.json.Json
import truerss.api.{JsonFormats, SourcesApi}
import truerss.db.DbLayer
import truerss.db.validation.SourceValidator
import truerss.dto._
import truerss.services.{FeedsService, NotFoundError, SourcesService, ValidationError}
import truerss.util.FeedSourceDtoModelImplicits
import zio.Task

class SourcesApiTests extends BaseApiTest {

  import FeedSourceDtoModelImplicits._
  import JsonFormats._

  private val path = "sources"

  private val sourceId_200 = 1L
  private val sourceId_404 = 10L
  private val sourceDto = Gen.genSource(Some(sourceId_200)).toView
  private val dtos = Vector(sourceDto)
  private val page = Page[FeedDto](1, Gen.genFeedDto :: Nil)
  private val newSource = Gen.genNewSource
  private val invalidNewSource = Gen.genNewSource
  private val errors = "boom" :: Nil
  private val updSource = Gen.genUpdSource(sourceId_200)
  private val invalidUpdSource = Gen.genUpdSource(sourceId_200)

  "sources api" should {
    "all" in new Test() {
      Get(api(s"$path/all")) ~> route ~> check {
        responseAs[String] ==== Json.toJson(dtos).toString
        status ==== StatusCodes.OK
      }
    }

    "one" should {
      "#ok" in new Test() {
        Get(api(s"$path/$sourceId_200")) ~> route ~> check {
          responseAs[String] ==== Json.toJson(sourceDto).toString
          status ==== StatusCodes.OK
        }
      }

      "#notFound" in new Test() {
        Get(api(s"$path/$sourceId_404")) ~> route ~> check {
          status ==== StatusCodes.NotFound
        }
      }
    }

    "latest" should {
      "simple" in new Test() {
        Get(api(s"$path/latest")) ~> route ~> check {
          responseAs[String] ==== Json.toJson(page).toString
          status ==== StatusCodes.OK
        }
        feedsService.savedOffset ==== 0
        feedsService.savedLimit ==== 100
      }

      "invalid offset" in new Test() {
        Get(api(s"$path/latest?offset=asd&limit=33")) ~> route ~> check {
          responseAs[String] ==== Json.toJson(page).toString
          status ==== StatusCodes.OK
        }
        feedsService.savedOffset ==== 0
        feedsService.savedLimit ==== 33
      }

      "invalid limit" in new Test() {
        Get(api(s"$path/latest?offset=10&limit=asd")) ~> route ~> check {
          responseAs[String] ==== Json.toJson(page).toString
          status ==== StatusCodes.OK
        }
        feedsService.savedOffset ==== 10
        feedsService.savedLimit ==== 100
      }
    }

    "feeds" should {
      "simple" in new Test() {
        Get(api(s"$path/$sourceId_200/feeds")) ~> route ~> check {
          responseAs[String] ==== Json.toJson(page).toString
          status ==== StatusCodes.OK
        }
        feedsService.savedSourceId ==== sourceId_200
        feedsService.savedFlag ==== true
        feedsService.savedOffset ==== 0
        feedsService.savedLimit ==== 100
      }

      "invalid offset" in new Test() {
        Get(api(s"$path/$sourceId_200/feeds?offset=asd&limit=10")) ~> route ~> check {
          responseAs[String] ==== Json.toJson(page).toString
          status ==== StatusCodes.OK
        }
        feedsService.savedSourceId ==== sourceId_200
        feedsService.savedFlag ==== true
        feedsService.savedOffset ==== 0
        feedsService.savedLimit ==== 10
      }

      "invalid limit" in new Test() {
        Get(api(s"$path/$sourceId_200/feeds?offset=10&limit=asd&unreadOnly=false")) ~> route ~> check {
          responseAs[String] ==== Json.toJson(page).toString
          status ==== StatusCodes.OK
        }
        feedsService.savedSourceId ==== sourceId_200
        feedsService.savedFlag ==== false
        feedsService.savedOffset ==== 10
        feedsService.savedLimit ==== 100
      }

      "invalid unreadOnly" in new Test() {
        Get(api(s"$path/$sourceId_200/feeds?offset=10&limit=10&unreadOnly=asd")) ~> route ~> check {
          responseAs[String] ==== Json.toJson(page).toString
          status ==== StatusCodes.OK
        }
        feedsService.savedSourceId ==== sourceId_200
        feedsService.savedFlag ==== true
        feedsService.savedOffset ==== 10
        feedsService.savedLimit ==== 10
      }
    }

    "find unread" in new Test() {
      Get(api(s"$path/unread/$sourceId_200")) ~> route ~> check {
        responseAs[String] ==== Json.toJson(page.resources).toString
        status ==== StatusCodes.OK
      }
      feedsService.savedSourceId ==== sourceId_200
    }

    "add" should {
      "#ok" in new Test() {
        Post(api(s"$path"), make(newSource)) ~> route ~> check {
          responseAs[String] ==== make(sourceDto)
          status ==== StatusCodes.OK
        }
      }

      "#invalid request" in new Test() {
        Post(api(s"$path"), make(sourceDto)) ~> route ~> check {
          status ==== StatusCodes.BadRequest
        }
      }

      "#not valid source" in new Test() {
        Post(api(s"$path"), make(invalidNewSource)) ~> route ~> check {
          status ==== StatusCodes.BadRequest
        }
      }
    }

    "update" should {
      "#ok" in new Test() {
        Put(api(s"$path/$sourceId_200"), make(updSource)) ~> route ~> check {
          responseAs[String] ==== make(sourceDto)
          status ==== StatusCodes.OK
        }
      }

      "#invalid request" in new Test() {
        Put(api(s"$path"), make(sourceDto)) ~> route ~> check {
          status ==== StatusCodes.BadRequest
        }
      }

      "#not valid source" in new Test() {
        Put(api(s"$path/$sourceId_200"), make(invalidUpdSource)) ~> route ~> check {
          status ==== StatusCodes.BadRequest
        }
      }
    }

    "delete" should {
      "#ok" in new Test() {
        Delete(api(s"$path/$sourceId_200")) ~> route ~> check {
          status ==== StatusCodes.NoContent
        }
      }

      "#not found" in new Test() {
        Delete(api(s"$path/$sourceId_404")) ~> route ~> check {
          status ==== StatusCodes.NotFound
        }
      }
    }

  }

  private class Test() extends BaseScope {

    val sourcesService = new SourcesService(
      null,
      null,
      null,
      null
    ) {
      var savedSourceId = 0L

      override def getAll: Task[Vector[SourceViewDto]] = Task.succeed(dtos)

      override def getSource(sourceId: Long): Task[SourceViewDto] = {
        sourceId match {
          case `sourceId_200` => Task.succeed(sourceDto)
          case _ => Task.fail(NotFoundError(sourceId_404))
        }
      }

      override def delete(sourceId: Long): Task[Unit] = {
        sourceId match {
          case `sourceId_200` => Task.succeed(sourceDto)
          case _ => Task.fail(NotFoundError(sourceId_404))
        }
      }

      override def addSource(dto: NewSourceDto): Task[SourceViewDto] = {
        if (dto == newSource) {
          Task.succeed(sourceDto)
        } else {
          Task.fail(ValidationError(errors))
        }
      }

      override def updateSource(sourceId: Long, dto: UpdateSourceDto): Task[SourceViewDto] = {
        if (dto == updSource) {
          Task.succeed(sourceDto)
        } else {
          Task.fail(ValidationError(errors))
        }
      }
    }

    val feedsService = new FeedsService(null) {
      var savedOffset = 0
      var savedLimit = 0
      var savedFlag = true
      var savedSourceId = 0L

      override def findBySource(sourceId: Long, unreadOnly: Boolean, offset: Int, limit: Int): Task[Page[FeedDto]] = {
        savedSourceId = sourceId
        savedOffset = offset
        savedLimit = limit
        savedFlag = unreadOnly
        Task.succeed(page)
      }

      override def findUnread(sourceId: Long): Task[Vector[FeedDto]] = {
        savedSourceId = sourceId
        Task.succeed(page.resources.toVector)
      }

      override def latest(offset: Int, limit: Int): Task[Page[FeedDto]] = {
        savedOffset = offset
        savedLimit = limit
        Task.succeed(page)
      }
    }

    override protected val route: Route = new SourcesApi(
      feedsService,
      sourcesService
    ).route

  }


}
