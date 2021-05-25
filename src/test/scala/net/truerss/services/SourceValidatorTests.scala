package net.truerss.services

import org.specs2.mutable.Specification
import net.truerss.{Gen, ZIOMaterializer}
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import truerss.db.driver.CurrentDriver
import truerss.db.validation.SourceValidator.TmpSource
import truerss.db.{DbLayer, SourcesDao}
import truerss.db.validation.{SourceUrlValidator, SourceValidator}
import truerss.dto.NewSourceDto
import truerss.plugins.ApplicationPlugins
import truerss.services.{ApplicationPluginsService, ValidationError}
import zio.Task

class SourceValidatorTests extends Specification with Mockito {

  import ZIOMaterializer._
  import SourceValidator._
  import SourceValidatorTests._

  "source validator" should {
    "validate interval" in {
      validateInterval(Gen.genNewSource.toTmp.copy(interval = -1)).e must beLeft(ValidationError(intervalError :: Nil))
      validateInterval(Gen.genNewSource.toTmp.copy(interval = 0)).e must beLeft(ValidationError(intervalError :: Nil))
      validateInterval(Gen.genNewSource.toTmp.copy(interval = 1)).e must beRight
    }

    "validate url" in {
      validateUrl(Gen.genNewSource.toTmp.copy(url = "asd")).e must beLeft(ValidationError(urlError))
      validateUrl(Gen.genNewSource.toTmp.copy(url = "http://example.com/rss")).e must beRight
    }

    "validate url length" in {
      val source = Gen.genNewSource.toTmp
      val invalidUrl = s"http://example${"a"*CurrentDriver.defaultLength}.com"
      validateUrlLength(source).e must beRight

      validateUrlLength(source.copy(url = invalidUrl)).e must beLeft(ValidationError(urlLengthError))
    }

    "validate name length" in {
      val source = Gen.genNewSource.toTmp
      val invalidName = "a"*CurrentDriver.defaultLength + "1"
      validateNameLength(source).e must beRight
      validateNameLength(source.copy(name = invalidName)).e must beLeft(ValidationError(nameLengthError))
    }

    "validate source" should {
      "url is not unique" in new Test() {
        val url = Gen.genUrl
        val newSource = Gen.genNewSource.copy(url = url)
        val tmp = newSource.toTmp
        sourceDao.findByUrl(url, None) returns Task.succeed(1)
        sourceDao.findByName(anyString, any) returns Task.succeed(0)
        sourceUrlValidator.validateUrl(tmp) returns Right(tmp)

        init()

        validator.validateSource(newSource).e must beLeft(ValidationError(urlError(tmp)))
      }

      "name is not unique" in new Test() {
        val name = "test"
        val newSource = Gen.genNewSource.copy(name = name)
        val tmp = newSource.toTmp
        sourceDao.findByUrl(anyString, any[Option[Long]]) returns Task.succeed(0)
        sourceDao.findByName(name, None) returns Task.succeed(1)
        sourceUrlValidator.validateUrl(tmp) returns Right(tmp)

        init()

        validator.validateSource(newSource).e must beLeft(ValidationError(nameError(tmp)))
      }

      "url is not rss" in new Test() {
        val error = "boom"
        val newSource = Gen.genNewSource
        val tmp = newSource.toTmp
        sourceDao.findByUrl(anyString, any[Option[Long]]) returns Task.succeed(0)
        sourceDao.findByName(anyString, any[Option[Long]]) returns Task.succeed(0)
        sourceUrlValidator.validateUrl(tmp) returns Left(error)

        init()

        validator.validateSource(newSource).e must beLeft(ValidationError(error))
      }

      "when ok" in new Test() {
        val newSource = Gen.genNewSource
        val tmp = newSource.toTmp
        sourceDao.findByUrl(anyString, any[Option[Long]]) returns Task.succeed(0)
        sourceDao.findByName(anyString, any[Option[Long]]) returns Task.succeed(0)
        sourceUrlValidator.validateUrl(tmp) returns Right(tmp)

        init()

        validator.validateSource(newSource).e must beRight
      }
    }
  }

  private class Test() extends Scope {
    val dbLayer = mock[DbLayer]
    val sourceDao = mock[SourcesDao]
    val sourceUrlValidator = mock[SourceUrlValidator]
    val appPluginsService = mock[ApplicationPluginsService]
    dbLayer.sourceDao returns sourceDao

    var validator: SourceValidator = _

    def init() = {
      validator = new SourceValidator(
        dbLayer,
        sourceUrlValidator,
        appPluginsService)
    }
  }

}

object SourceValidatorTests {
  implicit class NewSourceDtoExt(val x: NewSourceDto) extends AnyVal {
    def toTmp: TmpSource = {
      TmpSource(
        id = None,
        name = x.name,
        url = x.url,
        interval = x.interval
      )
    }
  }
}
