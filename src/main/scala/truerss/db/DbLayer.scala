package truerss.db

import slick.jdbc.JdbcBackend
import truerss.db.driver.CurrentDriver

import scala.concurrent.ExecutionContext

/**
  * Created by mike on 3.5.17.
  */
case class DbLayer(
                 db: JdbcBackend.DatabaseDef,
                 driver: CurrentDriver)(
                implicit val ec: ExecutionContext) {
  val sourceDao = new SourceDao(db)(ec, driver)
  val feedDao = new FeedDao(db)(ec, driver)
  val settingsDao = new PredefinedSettingsDao(db)(ec, driver)
  val userSettingsDao = new UserSettingsDao(db)(ec, driver)
}
