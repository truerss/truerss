package truerss.util

import truerss.db.{PredefinedSettings, RadioValue, SelectableValue, UserSettings}
import truerss.dto.{AvailableRadio, AvailableSelect, AvailableSetup, AvailableValue, CurrentValue, NewSetup, Setup, SetupKey}

import scala.reflect.ClassTag

object SettingsImplicits {
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

  implicit class UserSettingsExt(val x: UserSettings) extends AnyVal {
    def toSetup[T: ClassTag]: Setup[T] = {
      Vector(x.valueString, x.valueInt, x.valueBoolean).flatten.headOption match {
        case Some(t: T) => Setup[T](SetupKey(x.key, x.description), t)
        case _ => Setup.unknown[T](x.key)
      }
    }

    def toCurrentValue: CurrentValue[_] = {
      (x.valueString, x.valueInt, x.valueBoolean) match {
        case (Some(x), _, _) => CurrentValue[String](x)
        case (_, Some(x), _) => CurrentValue[Int](x)
        case (_, _, Some(x)) => CurrentValue[Boolean](x)
        case _ => CurrentValue.unknown[Any]
      }
    }
  }

  implicit class PredefinedSettingsExt(val x: PredefinedSettings) extends AnyVal {
    def toAvailableOptions: AvailableValue = {
      x.value match {
        case SelectableValue(predefined, _) => AvailableSelect(predefined)
        case RadioValue(_) => AvailableRadio
      }
    }
  }

  implicit class UserSettingExt(val x: UserSettings) extends AnyVal {
    def withDescription(description: String): UserSettings = {
      x.copy(description = description)
    }
  }

  def makeAvailableSetup(global: Iterable[PredefinedSettings],
                         user: Iterable[UserSettings]): Iterable[AvailableSetup[_]] = {
    val uMap = user.map { x => x.key -> x }.toMap
    global.map { x =>
      uMap.get(x.key) match {
        case Some(us) =>
          AvailableSetup(x.key, x.description, x.toAvailableOptions, us.toCurrentValue)

        case None =>
          val opts = x.toAvailableOptions
          AvailableSetup(x.key, x.description, opts,
            CurrentValue(x.value.defaultValue))
      }
    }
  }
}
