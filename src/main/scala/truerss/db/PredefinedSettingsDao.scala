package truerss.db

import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.CurrentDriver
import zio.Task

class PredefinedSettingsDao(val db: DatabaseDef)(implicit driver: CurrentDriver) {

  import JdbcTaskSupport._
  import driver.profile.api._
  import driver.query.predefinedSettings

  def getSettings: Task[Iterable[PredefinedSettings]] = {
    predefinedSettings.result ~> db
  }

  // for testing
  def insert(xs: Iterable[PredefinedSettings]): Task[Option[Int]] = {
    (predefinedSettings ++= xs) ~> db
  }

}
