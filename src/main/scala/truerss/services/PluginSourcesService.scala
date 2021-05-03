package truerss.services

import akka.event.EventStream
import truerss.db.validation.PluginSourceValidator
import truerss.db.{DbLayer, PluginSource}
import truerss.dto.{NewPluginSource, PluginSourceDto}
import truerss.plugins_discrovery.DiscoveryProvider
import truerss.services.actors.MainActor
import truerss.util.{EventStreamExt, PluginInstaller}
import zio.Task

class PluginSourcesService(
                           private val dbLayer: DbLayer,
                           private val pluginInstaller: PluginInstaller,
                           private val validator: PluginSourceValidator,
                           private val appPluginsService: ApplicationPluginsService,
                           private val stream: EventStream
                          ) extends DiscoveryProvider {

  import PluginSourcesService._
  import EventStreamExt._

  def installPlugin(urlToJar: String): Task[Unit] = {
    for {
      _ <- pluginInstaller.install(urlToJar)
      _ <- Task(appPluginsService.reload())
      _ <- stream.fire(MainActor.Restart)
    } yield()

  }

  def removePlugin(urlToJar: String): Task[Unit] = {
    for {
      _ <- pluginInstaller.remove(urlToJar)
      _ <- Task(appPluginsService.reload())
      _ <- stream.fire(MainActor.Restart)
    } yield ()

  }

  def availablePluginSources: Task[Iterable[PluginSourceDto]] = {
    for {
      all <- dbLayer.pluginSourcesDao.all
      pluginJars <- Task.foreachPar(all)(fetch)
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