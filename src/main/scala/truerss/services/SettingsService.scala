package truerss.services

import truerss.db.{DbLayer, PredefinedSettings, RadioValue, SelectableValue, UserSettings}
import truerss.dto.{AvailableRadio, AvailableSelect, AvailableSetup, AvailableValue, CurrentValue, NewSetup, Setup, SetupKey}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class SettingsService(dbLayer: DbLayer)(implicit ec: ExecutionContext) {

  import SettingsService._

  def getCurrentSetup: Future[Iterable[AvailableSetup[_]]] = {
    for {
      global <- dbLayer.settingsDao.getSettings
      user <- dbLayer.userSettingsDao.getSettings
    } yield {
      makeAvailableSetup(global, user)
    }
  }

  def updateSetup(userSettings: UserSettings): Future[Int] = {
    dbLayer.userSettingsDao.update(userSettings)
  }

  def updateSetups(userSettings: Iterable[UserSettings]): Future[Int] = {
    dbLayer.userSettingsDao.bulkUpdate(userSettings)
  }

  // the application layer dependency
  // we should use that in the application layer: for checking user-defined setup in every feature
  //
  def where[T: ClassTag](key: SetupKey, defaultValue: T): Future[Setup[T]] = {
    val default: Setup[T] = Setup[T](
      key = key,
      value = defaultValue
    )
    getCurrentSetup.map { xs =>
      xs.find(_.key == key.name).map { available =>
        Setup(
          key = key,
          value = available.value.value.asInstanceOf[T]
        )
      }.getOrElse(default)
    }
  }

}

object SettingsService {

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
        case RadioValue(currentState) => AvailableRadio(currentState)
      }
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