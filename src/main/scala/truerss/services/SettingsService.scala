package truerss.services

import org.slf4j.LoggerFactory
import truerss.db.{DbLayer, UserSettings}
import truerss.dto.{AvailableSetup, CurrentValue, NewSetup, Setup, SetupKey}
import truerss.util.SettingsImplicits
import zio.Task

import scala.reflect.ClassTag

class SettingsService(private val dbLayer: DbLayer) {

  import SettingsImplicits._
  import SettingsService._

  def getCurrentSetup: Task[Iterable[AvailableSetup[_]]] = {
    for {
      global <- dbLayer.settingsDao.getSettings
      user <- dbLayer.userSettingsDao.getSettings
    } yield makeAvailableSetup(global, user)
  }

  def updateSetups(newSetups: Iterable[NewSetup[_]]): Task[Unit] = {
    for {
      xs <- getCurrentSetup
      result <- Task.effectTotal(Reducer.reprocess(xs, newSetups))
      _ <- Task.fail(ValidationError(result.errors.toList)).when(result.errors.nonEmpty)
      _ <- dbLayer.userSettingsDao.bulkUpdate(result.setups)
    } yield ()
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

  import SettingsImplicits._

  private val logger = LoggerFactory.getLogger(getClass)

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

    def reprocess(xs: Iterable[AvailableSetup[_]], newSetups: Iterable[NewSetup[_]]): Reducer = {
      newSetups.foldLeft(Reducer.empty) { case (r, newSetup) =>
        xs.find(_.key == newSetup.key).map { result =>
          if (newSetup.isValidAgainst(result)) {
            val setup = newSetup.toUserSetup.withDescription(result.description)
            r.withSetup(setup)
          } else {
            logger.warn(s"Incorrect value: ${newSetup.key}:${newSetup.value}, options: ${result.options}")
            r.withError(buildError(newSetup.value))
          }
        }.getOrElse(r)
      }
    }

    def buildError(value: CurrentValue[_]): String = {
      s"Incorrect value: ${value.value}"
    }
  }

}