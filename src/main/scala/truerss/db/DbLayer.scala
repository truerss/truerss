package truerss.db

import slick.jdbc.JdbcBackend
import truerss.db.driver.CurrentDriver

import scala.concurrent.ExecutionContext

/**
  * Created by mike on 3.5.17.
  */
case class DbLayer(
                 db: JdbcBackend.DatabaseDef,
                 driver: CurrentDriver) {
  val sourceDao = new SourceDao(db)(driver)
  val feedDao = new FeedDao(db)(driver)
  val settingsDao = new PredefinedSettingsDao(db)(driver)
  val userSettingsDao = new UserSettingsDao(db)(driver)
}
