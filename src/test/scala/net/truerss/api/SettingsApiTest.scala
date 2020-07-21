package net.truerss.api

import net.truerss.Gen
import truerss.api._
import truerss.dto.AvailableSetup
import truerss.util.syntax
import truerss.services.management.SettingsManagement

class SettingsApiTest extends BaseApiTest {

  import JsonFormats._
  import syntax.future._

  private val setup = Gen.genSetup
  private val newSetup = Gen.genNewSetup

  private val sm = mock[SettingsManagement]
  sm.getCurrentSetup returns SettingsResponse(Iterable(setup)).toF
  sm.updateSetups(Iterable(newSetup)) returns ResponseHelpers.ok.toF

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
