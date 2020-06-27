package truerss.db

import slick.jdbc.JdbcBackend.DatabaseDef
import slick.sql.FixedSqlAction
import truerss.db.driver.CurrentDriver

import scala.concurrent.{ExecutionContext, Future}

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
    db.run(toStatement(settings))
  }

  def bulkUpdate(settings: Iterable[UserSettings]): Future[Int] = {
    val xs = settings.map(toStatement)
    Future.sequence(xs.map { o => db.run(o) }).map(_.sum)
  }

  def insert(xs: Iterable[UserSettings]): Future[Option[Int]] = {
    db.run {
      userSettings ++= xs
    }
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
