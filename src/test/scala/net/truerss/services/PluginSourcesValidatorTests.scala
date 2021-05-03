package net.truerss.services

import net.truerss.ZIOMaterializer
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import truerss.db.{DbLayer, PluginSourcesDao}
import truerss.db.validation.PluginSourceValidator
import truerss.dto.NewPluginSource
import truerss.services.ValidationError
import zio.Task

class PluginSourcesValidatorTests extends Specification with Mockito {

  import ZIOMaterializer._

  val url = "https://github.com/foo/bar/releases/tag/123"

  "validate/url" should {
    "be valid source" in new MyTest(1) {
      val tmp = "http://example.com"
      validator.validate(NewPluginSource(tmp)).e must beLeft(
        ValidationError(PluginSourceValidator.unknownSource(tmp))
      )
    }

    "be unique" in new MyTest(1) {
      validator.validate(NewPluginSource(url)).e must beLeft(
        ValidationError(PluginSourceValidator.notUniqueUrlError(url))
      )
    }

    "be valid url" in new MyTest(0) {
      validator.validate(NewPluginSource("asd")).e must beLeft(
        ValidationError(PluginSourceValidator.urlError)
      )
    }

    "success flow" in new MyTest(0) {
      val n = NewPluginSource(url)
      validator.validate(n).m ==== n
    }
  }

  private class MyTest(count: Int = 0) extends Scope {
    val dbLayer = mock[DbLayer]
    val dao = mock[PluginSourcesDao]
    dao.findByUrl(anyString) returns Task.succeed(count)
    dbLayer.pluginSourcesDao returns dao
    val validator = new PluginSourceValidator(dbLayer)
  }

}
