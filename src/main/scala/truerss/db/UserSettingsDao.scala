package truerss.db

import slick.jdbc.JdbcBackend.DatabaseDef
import slick.sql.FixedSqlAction
import truerss.db.driver.CurrentDriver
import zio.Task

class UserSettingsDao(val db: DatabaseDef)(implicit driver: CurrentDriver) {

  import JdbcTaskSupport._
  import driver.profile.api._
  import driver.query.userSettings

  def getSettings: Task[Iterable[UserSettings]] = {
    userSettings.result ~> db
  }

  def getByKey(key: String): Task[Option[UserSettings]] = {
    userSettings.filter(_.key === key).take(1).result.headOption ~> db
  }

  def update(settings: UserSettings): Task[Int] = {
    toStatement(settings) ~> db
  }

  def bulkUpdate(settings: Iterable[UserSettings]): Task[Unit] = {
    Task.collectAll(settings.map(toStatement).map { _ ~> db }).unit
  }

  def insert(xs: Iterable[UserSettings]): Task[Option[Int]] = {
    (userSettings ++= xs) ~> db
  }

  private def toStatement(settings: UserSettings): FixedSqlAction[Int, NoStream, Effect.Write] = {
    settings match {
      case UserSettings(key, description, Some(v), _, _) =>
        userSettings.map(a => (a.key, a.description, a.valueInt))
          .insertOrUpdate((key, description, Some(v)))
      case UserSettings(key, description, _, Some(v), _) =>
        userSettings.map(a => (a.key, a.description, a.valueBoolean))
          .insertOrUpdate((key, description, Some(v)))
      case UserSettings(key, description, _, _, Some(v)) =>
        userSettings.map(a => (a.key, a.description, a.valueString))
          .insertOrUpdate((key, description, Some(v)))
      case _ =>
        throw new IllegalArgumentException(s"Unknown settings: $settings")
    }
  }

}
