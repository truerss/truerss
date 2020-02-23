package truerss.db

import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.CurrentDriver

import scala.concurrent.{ExecutionContext, Future}

class PredefinedSettingsDao(val db: DatabaseDef)(implicit
                                                 val ec: ExecutionContext,
                                                 driver: CurrentDriver) {
  import driver.profile.api._
  import driver.query.predefinedSettings

  def getSettings: Future[Iterable[PredefinedSettings]] = {
    db.run(predefinedSettings.result)
  }
}
