package net.truerss.services

import org.specs2.mutable.Specification
import org.specs2.specification.AfterAll
import truerss.db.validation.PluginSourceValidator
import truerss.db.{DbLayer, PluginSource, PluginSourcesDao}
import truerss.dto.NewPluginSource
import truerss.services.{PluginSourcesService, ValidationError}
import truerss.util.{PluginInstaller, TaskImplicits}
import zio.{Cause, Task}

import java.io.File
import java.util.UUID

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
    }
  }
  private val validator = new PluginSourceValidator(dbLayer)

  private val service = new PluginSourcesService(
    dbLayer = dbLayer,
    pluginInstaller = installer,
    validator = validator
  )

  private val url = "https://github.com/truerss/plugins/releases/tag/1.0.0"

  "service" should {
    "follow api" in {
      // get available
      service.availablePluginSources.materialize must have size 0
      // add new one
      val added = service.addNew(NewPluginSource(url)).materialize
      added.url ==== url
      added.plugins must have size 4

      // install one
      val available = service.availablePluginSources.materialize
      available must have size 1
      available.head.plugins must have size 4

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

      (new File(fileName)).exists() must beTrue

      // todo remove one plugin and plugin source

      success
    }
  }

  override def afterAll(): Unit = {
    installedPlugins.foreach(x => new File(x).delete())
    tempDir.delete()
  }
}
