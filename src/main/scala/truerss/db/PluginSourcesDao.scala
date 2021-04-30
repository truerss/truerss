package truerss.db

import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.CurrentDriver
import zio.Task

class PluginSourcesDao(val db: DatabaseDef)(implicit
                                            driver: CurrentDriver
) {
  import JdbcTaskSupport._
  import driver.profile.api._
  import driver.query.pluginSources

  def all: Task[Seq[PluginSource]] = {
    pluginSources.result ~> db
  }

  def insert(p: PluginSource): Task[Long] = {
    ((pluginSources returning pluginSources.map(_.id)) += p) ~> db
  }

  def findByUrl(url: String): Task[Int] = {
    pluginSources.filter(_.url === url).length.result ~> db
  }

}
