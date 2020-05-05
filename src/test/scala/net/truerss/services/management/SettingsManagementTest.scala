package net.truerss.services.management

import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import truerss.api.{BadRequestResponse, SettingResponse}
import truerss.db.UserSettings
import truerss.dto.{AvailableSelect, AvailableSetup, CurrentValue}
import truerss.services.SettingsService
import truerss.services.management.SettingsManagement
import truerss.util.Util.ResponseHelpers

import scala.concurrent.Future

class SettingsManagementTest(implicit val ee: ExecutionEnv) extends Specification with Mockito {

  import SettingsManagement._

  private val setup1 = Gen.genSetup
  private val setup2 = Gen.genSetup
  private val setup3 = Gen.genSetup
  private val newSetup1 = Gen.genNewSetup
  private val newSetup2 = Gen.genNewSetup.copy(key = setup1.key)

  private val ss = mock[SettingsService]
  ss.getCurrentSetup returns f(Iterable(setup1, setup2, setup3))
  ss.updateSetup(any[UserSettings]) returns f(1)

  private val sm = new SettingsManagement(ss)

  "settings management" should {
    "update, validate keys" in {
      sm.updateSetup(newSetup1) must be_==(BadRequestResponse(s"Unknown key: ${newSetup1.key}")).await
      there was no(ss).updateSetup(newSetup1.toUserSetup)
    }

    "update, should be ok, if key is exists" in {
      sm.updateSetup(newSetup2) must haveClass[SettingResponse[_]].await
      there was one(ss)
    }

    "validation" in {
      val newSetup = newSetup2.copy(value = CurrentValue(1000))
      val available = AvailableSetup(
        key = "test",
        description = "test",
        options = AvailableSelect(Iterable(1, 2, 3)),
        value = CurrentValue(1)
      )
      newSetup.isValidAgainst(available) must beFalse
      newSetup.copy(value = CurrentValue(3)).isValidAgainst(available) must beTrue
    }

  }

  def f[T](x: T) = Future.successful(x)

}
