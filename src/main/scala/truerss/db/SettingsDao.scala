package truerss.db

import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.{CurrentDriver, Settings}

import scala.concurrent.{ExecutionContext, Future}

class SettingsDao(val db: DatabaseDef)(implicit
                                       val ec: ExecutionContext,
                                       driver: CurrentDriver) {
  import driver.profile.api._
  import driver.query.settings

  def getSettings: Future[Iterable[Settings]] = {
    db.run(settings.result)
  }


}
