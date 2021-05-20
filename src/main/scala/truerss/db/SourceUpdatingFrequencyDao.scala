package truerss.db

import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.CurrentDriver
import zio.Task

class SourceUpdatingFrequencyDao(val db: DatabaseDef)(implicit driver: CurrentDriver) {

  import JdbcTaskSupport._
  import driver.profile.api._
  import driver.query.sourceUpdateFrequencies

  def updateFrequency(model: SourceUpdatingFrequency): Task[Int] = {
    sourceUpdateFrequencies.map { x => (x.sourceId, x.perDay) }
      .insertOrUpdate((model.sourceId, model.perDay)) ~> db
  }

}
