package net.truerss.services

import akka.http.scaladsl.model.StatusCodes
import truerss.api._
import truerss.dto.{AvailableSelect, AvailableSetup, CurrentValue, NewSetup}
import truerss.services.management.SettingsManagement
import truerss.util.Util.ResponseHelpers

class SettingsApiTest extends BaseApiTest {

  import JsonFormats._

  private val setup = AvailableSetup(
    key = "test",
    description = "test",
    options = AvailableSelect(Iterable(1, 2, 3)),
    value = CurrentValue(1)
  )
  private val newSetup = NewSetup("test", CurrentValue(1))

  private val sm = mock[SettingsManagement]
  sm.getCurrentSetup returns f(SettingsResponse(Iterable(setup)))
  sm.updateSetup(newSetup) returns f(ResponseHelpers.ok)

  protected override val r = new SettingsApi(sm).route

  private val url = "/api/v1/settings"

  "settings api" should {
    "return current setup" in {
      checkR[Iterable[AvailableSetup[_]]](Get(s"$url/current"), Iterable(setup))
      there was one(sm).getCurrentSetup
    }

    "update setup" in {
      checkR(Put(s"$url", newSetupFormat.writes(newSetup).toString()), StatusCodes.OK)
      there was one(sm).updateSetup(newSetup)
    }
  }

}
