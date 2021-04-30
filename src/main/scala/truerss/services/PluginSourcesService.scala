package truerss.services

import truerss.db.validation.PluginSourceValidator
import truerss.db.{DbLayer, PluginSource}
import truerss.dto.{NewPluginSource, PluginSourceDto}
import zio.Task

class PluginSourcesService(private val dbLayer: DbLayer,
                           private val validator: PluginSourceValidator
                          ) {

  import PluginSourcesService._

  def availablePluginSources: Task[Iterable[PluginSourceDto]] = {
    dbLayer.pluginSourcesDao.all.map { xs => xs.flatMap(_.toDto) }
  }

  def addNew(newPluginSource: NewPluginSource): Task[PluginSourceDto] = {
    for {
      _ <- validator.validate(newPluginSource)
      id <- dbLayer.pluginSourcesDao.insert(newPluginSource.toDb)
    } yield PluginSourceDto(
      id = id,
      url = newPluginSource.url
    )
  }

}

object PluginSourcesService {

  implicit class NewPluginSourceExt(val p: NewPluginSource) extends AnyVal {
    def toDb: PluginSource = {
      PluginSource(
        id = None,
        url = p.url
      )
    }
  }

  implicit class PluginSourceExt(val p: PluginSource) extends AnyVal {
    def toDto: Option[PluginSourceDto] = {
      p.id.map { x =>
        PluginSourceDto(
          id = x,
          url = p.url
        )
      }
    }
  }
}