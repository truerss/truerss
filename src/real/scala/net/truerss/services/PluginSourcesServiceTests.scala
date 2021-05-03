package net.truerss.services

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import truerss.db.validation.PluginSourceValidator
import truerss.db.{DbLayer, PluginSource, PluginSourcesDao}
import truerss.dto.NewPluginSource
import truerss.services.actors.MainActor
import truerss.services.{ApplicationPluginsService, PluginNotFoundError, PluginSourcesService, ValidationError}
import truerss.util.{PluginInstaller, TaskImplicits}
import zio.Task

import java.io.File
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration._

class PluginSourcesServiceTests extends Specification with AfterAll {

  import TaskImplicits._

  private val tempDir = File.createTempFile(s"test-truerss-${UUID.randomUUID()}", "")
  tempDir.delete()
  tempDir.mkdir()
  private val installedPlugins = scala.collection.mutable.ArrayBuffer[String]()

  println(s"-------> path to config: ${tempDir.getPath}")

  private val installer = new PluginInstaller(tempDir.getPath)

  private val dbLayer = new DbLayer(null, null) {
    override val pluginSourcesDao: PluginSourcesDao = new PluginSourcesDao(null)(null) {
      private val lastId = 0L
      private val hm = scala.collection.mutable.HashMap[Long, PluginSource]()
      override def all: Task[Seq[PluginSource]] = {
        Task.effectTotal(hm.values.toSeq)
      }

      override def insert(p: PluginSource): Task[Long] = {
        val id = lastId + 1
        hm += id -> p.copy(id = Some(id))
        Task.effectTotal(id)
      }

      override def findByUrl(url: String): Task[Int] = {
        Task.effectTotal(hm.values.count(p => p.url == url))
      }

      override def delete(id: Long): Task[Int] = {
        hm -= id
        Task.effectTotal(1)
      }
    }
  }
  private val validator = new PluginSourceValidator(dbLayer)

  private var reloadTimes = 0
  private val appPluginService = new ApplicationPluginsService(
    tempDir.getPath, ConfigFactory.empty()
  ) {
    override def reload(): Unit = {
      reloadTimes = reloadTimes + 1
      super.reload()
    }
  }
  appPluginService.reload()

  private var reloadActorTimes = 0
  private val service = new PluginSourcesService(
    dbLayer = dbLayer,
    pluginInstaller = installer,
    validator = validator,
    appPluginsService = appPluginService,
    stream = null
  ) {
    override def fire(x: Any): Task[Unit] = {
      reloadActorTimes = reloadActorTimes + 1
      Task.unit
    }
  }

  private val url = "https://github.com/truerss/plugins/releases/tag/1.0.0"

  "service" should {
    "follow api" in {
      val total = 5
      // get available
      service.availablePluginSources.materialize must have size 0
      // add new one
      val added = service.addNew(NewPluginSource(url)).materialize
      added.url ==== url
      added.plugins must have size total

      // install one
      val available = service.availablePluginSources.materialize
      available must have size 1
      available.head.plugins must have size total

      appPluginService.view.materialize.size ==== 0

      // add again
      (for {
        x <- service.addNew(NewPluginSource(url)).fold(
          err => err.asInstanceOf[ValidationError].errors must contain(
            PluginSourceValidator.notUniqueUrlError(url)
          ),
          _ => failure("bad branch")
        )
      } yield x).materialize

      // install plugin
      val first = available.head.plugins.head
      service.installPlugin(first).materialize
      val fileName = PluginInstaller.toFilePath(tempDir.getPath, first)
      installedPlugins += fileName

      new File(fileName).exists() must beTrue

      appPluginService.view.materialize.size ==== 1

      // remove plugin source
      service.deletePluginSource(available.head.id).materialize
      service.availablePluginSources.materialize must be empty

      // plugin should be stay in the same place
      appPluginService.view.materialize.size ==== 1

      // remove plugin
      (for { _ <- service.removePlugin("foo-bar.jar").fold(
        err => err must haveClass[PluginNotFoundError.type],
        _ => failure("bad branch")
      )} yield ()).materialize

      appPluginService.view.materialize.size ==== 1

      service.removePlugin(first).materialize
      (new File(fileName)).exists() must beFalse
      appPluginService.view.materialize.size ==== 0
      reloadTimes ==== 3
      reloadActorTimes ==== 2
    }
  }

  override def afterAll(): Unit = {
    installedPlugins.foreach(x => new File(x).delete())
    tempDir.delete()
  }
}
