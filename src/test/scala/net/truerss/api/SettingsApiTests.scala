package net.truerss.api

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import play.api.libs.json.Json
import truerss.api.{HttpApi, JsonFormats, SettingsApi}
import truerss.db.DbLayer
import truerss.dto.{AvailableRadio, AvailableSetup, CurrentValue, NewSetup}
import truerss.services.{SettingsService, ValidationError}
import zio.Task

class SettingsApiTests extends BaseApiTest {

  import JsonFormats._

  private val path = "settings"
  private val errors = "boom" :: Nil
  private val newTestSetups: Iterable[NewSetup[_]] = NewSetup("test", CurrentValue(false)) :: Nil
  private val invalidNewSetup: Iterable[NewSetup[_]] = NewSetup("test#1", CurrentValue(false)) :: Nil
  private val current: Iterable[AvailableSetup[_]] = AvailableSetup("test", "test", AvailableRadio(true), CurrentValue(false)) :: Nil

  "settings api" should {
    "get current setup" in new Test() {
      Get(api(s"$path/current")) ~> route ~> check {
        responseAs[String] ===  Json.toJson(current).toString
        status ==== StatusCodes.OK
      }
    }

    "update setups" in new Test() {
      Put(api(path), Json.toJson(newTestSetups).toString) ~> route ~> check {
        status ==== StatusCodes.NoContent
      }
    }

    "return error when setups is not valid" in new Test() {
      Put(api(path), Json.toJson(invalidNewSetup).toString) ~> route ~> check {
        responseAs[String] ==== HttpApi.printErrors(errors)
        status ==== StatusCodes.BadRequest
      }
    }
  }

  private class Test() extends BaseScope {
    val service = new SettingsService(null) {
      override def getCurrentSetup: Task[Iterable[AvailableSetup[_]]] = {
        Task.succeed(current)
      }

      override def updateSetups(newSetups: Iterable[NewSetup[_]]): Task[Unit] = {
        if (newTestSetups == newSetups) {
          Task.succeed(())
        } else {
          Task.fail(ValidationError(errors))
        }
      }
    }
    override protected val route: Route = new SettingsApi(service).route
  }

}
