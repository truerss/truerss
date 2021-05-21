package truerss.db

import slick.jdbc.JdbcBackend
import truerss.db.driver.CurrentDriver

/**
  * Created by mike on 3.5.17.
  */
case class DbLayer(
                 db: JdbcBackend.DatabaseDef,
                 driver: CurrentDriver) {
  val sourceDao = new SourcesDao(db)(driver)
  val feedDao = new FeedsDao(db)(driver)
  val settingsDao = new PredefinedSettingsDao(db)(driver)
  val userSettingsDao = new UserSettingsDao(db)(driver)
  val pluginSourcesDao = new PluginSourcesDao(db)(driver)
  val sourceStatusesDao = new SourceStatusesDao(db)(driver)
}
