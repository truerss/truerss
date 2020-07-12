package truerss.db

import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.CurrentDriver
import zio.Task

import scala.concurrent.{ExecutionContext, Future}

class PredefinedSettingsDao(val db: DatabaseDef)(implicit
                                                 val ec: ExecutionContext,
                                                 driver: CurrentDriver) {
  import driver.profile.api._
  import driver.query.predefinedSettings

  def getSettings: Task[Iterable[PredefinedSettings]] = {
    Task.fromFuture { implicit ec => db.run(predefinedSettings.result) }
  }

  // for testing
  def insert(xs: Iterable[PredefinedSettings]): Task[Option[Int]] = {
    Task.fromFuture { implicit ec =>
      db.run {
        predefinedSettings ++= xs
      }
    }
  }

}
