package truerss.services.management

import truerss.api.{BadRequestResponse, SettingResponse, SettingsResponse}
import truerss.db.UserSettings
import truerss.dto.{AvailableSelect, AvailableSetup, CurrentValue, NewSetup}
import truerss.services.SettingsService
import truerss.util.syntax.future._

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class SettingsManagement(val settingsService: SettingsService)
                        (implicit val ec: ExecutionContext) extends BaseManagement {

  import SettingsManagement._

  def getCurrentSetup: R = {
    settingsService.getCurrentSetup.map(SettingsResponse(_))
  }

  def updateSetup[T: ClassTag](newSetup: NewSetup[T]): R = {
    settingsService.getCurrentSetup.flatMap { xs =>
      xs.find(_.key == newSetup.key).map { result =>
        if (newSetup.isValidAgainst(result)) {
          val setup = newSetup.toUserSetup.withDescription(result.description)
          settingsService.updateSetup(setup).map { x =>
            logger.debug(s"Update setup: ${newSetup.key} with ${newSetup.value}")
            SettingResponse(result.copy(value = newSetup.value))
          }
        } else {
          logger.warn(s"Incorrect value: ${newSetup.key}:${newSetup.value}, options: ${result.options}")
          BadRequestResponse(s"Incorrect value: ${newSetup.value}").toF
        }
      }.getOrElse(BadRequestResponse(s"Unknown key: ${newSetup.key}").toF)
    }
  }

}

object SettingsManagement {

  implicit class NewSetupExt[T: ClassTag](val x: NewSetup[T]) {
    def isValidAgainst[R](availableSetup: AvailableSetup[R]): Boolean = {
      availableSetup.options match {
        case AvailableSelect(xs) =>
          x.value match {
            case CurrentValue(v: Int) =>
              xs.toVector.contains(v)

            case _ => false
          }

        case _ => true
      }
    }

    def toUserSetup: UserSettings = {
      val tmp = UserSettings(
        key = x.key,
        description = x.key,
        valueBoolean = None,
        valueInt = None,
        valueString = None
      )
      x.value match {
        case CurrentValue(value: Int) =>
          tmp.copy(valueInt = Some(value))
        case CurrentValue(value: String) =>
          tmp.copy(valueString = Some(value))
        case CurrentValue(value: Boolean) =>
          tmp.copy(valueBoolean = Some(value))
        case x =>
          throw new IllegalStateException(s"Unknown type: $x")
      }
    }
  }

  implicit class UserSettingExt(val x: UserSettings) extends AnyVal {
    def withDescription(description: String): UserSettings = {
      x.copy(description = description)
    }
  }

}
