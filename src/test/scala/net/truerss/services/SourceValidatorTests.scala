package net.truerss.services

import org.specs2.mutable.Specification
import net.truerss.{Gen, ZIOMaterializer}
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import truerss.db.{DbLayer, SourceDao}
import truerss.db.validation.{SourceUrlValidator, SourceValidator}
import truerss.dto.{ApplicationPlugins, NewSourceDto}
import truerss.services.ValidationError
import zio.Task

class SourceValidatorTests extends Specification with Mockito {

  import ZIOMaterializer._
  import SourceValidator._

  "source validator" should {
    "validate interval" in {
      validateInterval(Gen.genNewSource.copy(interval = -1)).e must beLeft(ValidationError(intervalError :: Nil))
      validateInterval(Gen.genNewSource.copy(interval = 0)).e must beLeft(ValidationError(intervalError :: Nil))
      validateInterval(Gen.genNewSource.copy(interval = 1)).e must beRight
    }

    "validate url" in {
      validateUrl(Gen.genNewSource.copy(url = "asd")).e must beLeft(ValidationError(urlError :: Nil))
      validateUrl(Gen.genNewSource.copy(url = "http://example.com/rss")).e must beRight
    }

    "validate source" should {
      "url is not unique" in new Test() {
        val url = Gen.genUrl
        val newSource = Gen.genNewSource.copy(url = url)
        sourceDao.findByUrl(url, None) returns Task.succeed(1)
        sourceDao.findByName(anyString, any[Option[Long]]) returns Task.succeed(0)
        sourceUrlValidator.validateUrl(newSource) returns Right(newSource)

        init()

        validator.validateSource(newSource).e must beLeft(ValidationError(urlError(newSource)))
      }

      "name is not unique" in new Test() {
        val name = "test"
        val newSource = Gen.genNewSource.copy(name = name)
        sourceDao.findByUrl(anyString, any[Option[Long]]) returns Task.succeed(0)
        sourceDao.findByName(name, None) returns Task.succeed(1)
        sourceUrlValidator.validateUrl(newSource) returns Right(newSource)

        init()

        validator.validateSource(newSource).e must beLeft(ValidationError(nameError(newSource)))
      }

      "url is not rss" in new Test() {
        val error = "boom"
        val newSource = Gen.genNewSource
        sourceDao.findByUrl(anyString, any[Option[Long]]) returns Task.succeed(0)
        sourceDao.findByName(anyString, any[Option[Long]]) returns Task.succeed(0)
        sourceUrlValidator.validateUrl(newSource) returns Left(error)

        init()

        validator.validateSource(newSource).e must beLeft(ValidationError(error))
      }

      "when ok" in new Test() {
        val error = "boom"
        val newSource = Gen.genNewSource
        sourceDao.findByUrl(anyString, any[Option[Long]]) returns Task.succeed(0)
        sourceDao.findByName(anyString, any[Option[Long]]) returns Task.succeed(0)
        sourceUrlValidator.validateUrl(newSource) returns Right(newSource)

        init()

        validator.validateSource(newSource).e must beRight
      }
    }

    "filterValid" should {
      "name is not unique" in new Test() {
        val newSource = Gen.genNewSource
        val name = newSource.name
        val url = newSource.url
        sourceDao
          .findByUrlsAndNames(Seq(url), Seq(name)) returns Task.succeed(Seq(("url", name)))
        appPlugins.matchUrl(url) returns false
        sourceUrlValidator.validateUrls(Seq.empty[NewSourceDto]) returns Task.succeed(Seq.empty)

        init()

        validator.filterValid(Iterable(newSource)).m must be empty
      }

      "url is not unique" in new Test() {
        val newSource = Gen.genNewSource
        val name = newSource.name
        val url = newSource.url
        sourceDao
          .findByUrlsAndNames(Seq(url), Seq(name)) returns Task.succeed(Seq((url, "name")))
        appPlugins.matchUrl(url) returns false
        sourceUrlValidator.validateUrls(Seq.empty[NewSourceDto]) returns Task.succeed(Seq.empty)

        init()

        validator.filterValid(Iterable(newSource)).m must be empty
      }

      "urls are not valid" in new Test() {
        val newSource = Gen.genNewSource
        val name = newSource.name
        val url = newSource.url
        sourceDao
          .findByUrlsAndNames(Seq(url), Seq(name)) returns Task.succeed(Seq())
        appPlugins.matchUrl(url) returns false
        sourceUrlValidator.validateUrls(Seq(newSource)) returns Task.succeed(Seq.empty)

        init()

        validator.filterValid(Iterable(newSource)).m must be empty
      }

      "do not call validateUrls for plugins" in new Test() {
        val newSource = Gen.genNewSource
        val name = newSource.name
        val url = newSource.url
        sourceDao
          .findByUrlsAndNames(Seq(url), Seq(name)) returns Task.succeed(Seq())
        appPlugins.matchUrl(url) returns true
        sourceUrlValidator.validateUrls(Seq.empty[NewSourceDto]) returns Task.succeed(Seq.empty)

        init()

        validator.filterValid(Iterable(newSource)).m ==== Iterable(newSource)
      }

      "return only valid sources" in new Test() {
        val newSourceNotUniqueName = Gen.genNewSource
        val newSourceNotUniqueUrl = Gen.genNewSource
        val newSourceInvalidUrl = Gen.genNewSource
        val newSourcePlugin = Gen.genNewSource
        val newSourceValid = Gen.genNewSource

        sourceDao.findByUrlsAndNames(
          Seq(
            newSourceNotUniqueName.url,
            newSourceNotUniqueUrl.url,
            newSourceInvalidUrl.url,
            newSourcePlugin.url,
            newSourceValid.url
          ),
          Seq(
            newSourceNotUniqueName.name,
            newSourceNotUniqueUrl.name,
            newSourceInvalidUrl.name,
            newSourcePlugin.name,
            newSourceValid.name
          )
        ) returns Task.succeed(Seq(
          (newSourceNotUniqueUrl.url, "test"),
          ("test", newSourceNotUniqueName.name)
        ))

        appPlugins.matchUrl(anyString) returns false
        appPlugins.matchUrl(newSourcePlugin.url) returns true

        sourceUrlValidator.validateUrls(any[Seq[NewSourceDto]]) returns Task.succeed(Seq(
          newSourceValid
        ))

        init()

        validator.filterValid(Iterable(
          newSourceNotUniqueName,
          newSourceNotUniqueUrl,
          newSourceInvalidUrl,
          newSourcePlugin,
          newSourceValid
        )).m ==== Iterable(newSourcePlugin, newSourceValid)
      }
    }
  }

  private class Test() extends Scope {
    val dbLayer = mock[DbLayer]
    val sourceDao = mock[SourceDao]
    val sourceUrlValidator = mock[SourceUrlValidator]
    val appPlugins = mock[ApplicationPlugins]
    dbLayer.sourceDao returns sourceDao

    var validator: SourceValidator = _

    def init() = {
      validator = new SourceValidator(dbLayer, sourceUrlValidator, appPlugins)
    }

  }

}
