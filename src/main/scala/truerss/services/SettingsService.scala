package truerss.services

import org.slf4j.LoggerFactory
import truerss.db.{DbLayer, PredefinedSettings, RadioValue, SelectableValue, UserSettings}
import truerss.dto.{AvailableRadio, AvailableSelect, AvailableSetup, AvailableValue, CurrentValue, NewSetup, Setup, SetupKey}
import zio.Task

import scala.reflect.ClassTag

class SettingsService(dbLayer: DbLayer) {

  import SettingsService._

  private val logger = LoggerFactory.getLogger(getClass)

  def getCurrentSetup: Task[Iterable[AvailableSetup[_]]] = {
    for {
      global <- dbLayer.settingsDao.getSettings
      user <- dbLayer.userSettingsDao.getSettings
    } yield makeAvailableSetup(global, user)
  }

  def updateSetups(newSetups: Iterable[NewSetup[_]]): Task[Unit] = {
    getCurrentSetup.flatMap { xs =>
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
        Task.fail(ValidationError(result.errors.toList))
      } else {
        update(result.setups)
      }
    }
  }

  private def update(userSettings: Iterable[UserSettings]): Task[Unit] = {
    dbLayer.userSettingsDao.bulkUpdate(userSettings).map(_ => ())
  }

  // the application layer dependency
  // we should use that in the application layer: for checking user-defined setup in every feature
  //
  def where[T: ClassTag](key: SetupKey, defaultValue: T): Task[Setup[T]] = {
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