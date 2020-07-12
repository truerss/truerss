package truerss.services.management

import truerss.api.{BadRequestResponse, Ok, SettingsResponse}
import truerss.db.UserSettings
import truerss.dto.{AvailableSelect, AvailableSetup, CurrentValue, NewSetup}
import truerss.services.SettingsService
import zio.Task

import scala.concurrent.ExecutionContext
import scala.reflect.ClassTag

class SettingsManagement(val settingsService: SettingsService)
                        (implicit val ec: ExecutionContext) extends BaseManagement {

  import SettingsManagement._

  def getCurrentSetup: Z = {
    settingsService.getCurrentSetup.map(SettingsResponse(_))
  }

  def updateSetups(newSetups: Iterable[NewSetup[_]]): Z = {
    settingsService.getCurrentSetup.flatMap { xs =>
      val result = newSetups.foldLeft(Reducer.empty) { case (r, newSetup) =>
        xs.find(_.key == newSetup.key).map { result =>
          if (newSetup.isValidAgainst(result)) {
            val setup = newSetup.toUserSetup.withDescription(result.description)
            r.withSetup(setup)
          } else {
            logger.warn(s"Incorrect value: ${newSetup.key}:${newSetup.value}, options: ${result.options}")
            r.withError(s"Incorrect value: ${newSetup.value}")
          }
        }.getOrElse(r)
      }
      if (result.isEmpty) {
        Task.succeed(BadRequestResponse(result.errors.mkString(", ")))
      } else {
        settingsService.updateSetups(result.setups).map { x =>
          logger.debug(s"Updated: $x setups")
          Ok
        }
      }
    }
  }

}

object SettingsManagement {

  case class Reducer(setups: Vector[UserSettings], errors: Vector[String]) {
    def withError(error: String): Reducer = {
      copy(errors = errors :+ error)
    }

    def withSetup(setup: UserSettings): Reducer = {
      copy(setups = setups :+ setup)
    }

    def isEmpty: Boolean = {
      setups.isEmpty
    }
  }

  object Reducer {
    val empty = Reducer(Vector.empty, Vector.empty)
  }

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
