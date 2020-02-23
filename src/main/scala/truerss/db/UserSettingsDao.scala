package truerss.db

import slick.jdbc.JdbcBackend.DatabaseDef
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
    db.run(userSettings.filter(x => x.key === key).take(1).result)
      .map(_.headOption)
  }

}
