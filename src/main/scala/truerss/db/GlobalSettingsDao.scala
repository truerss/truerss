package truerss.db

import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.{CurrentDriver, GlobalSettings}

import scala.concurrent.{ExecutionContext, Future}

class GlobalSettingsDao(val db: DatabaseDef)(implicit
                                             val ec: ExecutionContext,
                                             driver: CurrentDriver) {
  import driver.profile.api._
  import driver.query.globalSettings

  def getGlobalSettings: Future[Iterable[GlobalSettings]] = {
    db.run(globalSettings.result)
  }

}
