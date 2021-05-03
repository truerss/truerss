package truerss.services

import truerss.db.validation.PluginSourceValidator
import truerss.db.{DbLayer, PluginSource}
import truerss.dto.{NewPluginSource, PluginSourceDto}
import truerss.plugins_discrovery.DiscoveryProvider
import truerss.util.PluginInstaller
import zio.Task

class PluginSourcesService(
                           val dbLayer: DbLayer,
                           val pluginInstaller: PluginInstaller,
                           val validator: PluginSourceValidator,
                           val appPluginsService: ApplicationPluginsService,
                          ) extends DiscoveryProvider {

  import PluginSourcesService._

  private val retryCount = 3

  def installPlugin(urlToJar: String): Task[Unit] = {
    for {
      _ <- pluginInstaller.install(urlToJar)
      _ <- Task(appPluginsService.reload())
    } yield()
  }

  def removePlugin(urlToJar: String): Task[Unit] = {
    for {
      _ <- pluginInstaller.remove(urlToJar)
      _ <- Task(appPluginsService.reload())
    } yield ()
  }

  def availablePluginSources: Task[Iterable[PluginSourceDto]] = {
    for {
      all <- dbLayer.pluginSourcesDao.all
      pluginJars <- Task.foreachPar(all)(fetch(_).retryN(retryCount))
    } yield pluginJars
  }

  // @note I remove without plugins for the plugin source
  def deletePluginSource(id: Long): Task[Unit] = {
    dbLayer.pluginSourcesDao.delete(id).unit
  }

  def addNew(newPluginSource: NewPluginSource): Task[PluginSourceDto] = {
    for {
      _ <- validator.validate(newPluginSource)
      id <- dbLayer.pluginSourcesDao.insert(newPluginSource.toDb)
      empty = PluginSource(id = Some(id), url = newPluginSource.url)
      result <- fetch(empty)
    } yield result
  }

  private def fetch(p: PluginSource): Task[PluginSourceDto] = {
    for {
      jars <- fetch(p.url)
    } yield PluginSourceDto(
      id = p.id.get, // ^
      url = p.url,
      plugins = jars.map(_.url)
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
    def toEmptyDto: Option[PluginSourceDto] = {
      p.id.map { x =>
        PluginSourceDto(
          id = x,
          url = p.url,
          plugins = Vector.empty
        )
      }
    }
  }
}