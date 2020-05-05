package truerss.db

import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.CurrentDriver

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class UserSettingsDao(val db: DatabaseDef)(implicit
                                           val ec: ExecutionContext,
                                           driver: CurrentDriver) {

  import driver.profile.api._
  import driver.query.userSettings

  def getSettings: Future[Iterable[UserSettings]] = {
    db.run(userSettings.result)
  }

  def getByKey(key: String): Future[Option[UserSettings]] = {
    db.run(userSettings.filter(_.key === key).take(1).result)
      .map(_.headOption)
  }

  def update(settings: UserSettings): Future[Int] = {
    val statement = settings match {
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
        throw new IllegalStateException(s"Incorrect settings: $settings")
    }
    db.run(statement)
  }

  def insert(xs: Iterable[UserSettings]): Future[Option[Int]] = {
    db.run {
      userSettings ++= xs
    }
  }

}
