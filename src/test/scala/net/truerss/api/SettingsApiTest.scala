package net.truerss.api

import akka.http.scaladsl.model.StatusCodes
import net.truerss.Gen
import truerss.api._
import truerss.dto.{AvailableSelect, AvailableSetup, CurrentValue, NewSetup}
import truerss.services.management.{ResponseHelpers, SettingsManagement}

class SettingsApiTest extends BaseApiTest {

  import JsonFormats._

  private val setup = Gen.genSetup
  private val newSetup = Gen.genNewSetup

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

//    "update setup" in {
//      checkR(Put(s"$url", newSetupFormat.writes(newSetup).toString()), StatusCodes.OK)
//      there was one(sm).updateSetup[Int](newSetup)
//    }
  }

}
