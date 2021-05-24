package net.truerss.services

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import truerss.db.validation.SourceUrlValidator
import net.truerss.{Gen, ZIOMaterializer}
import truerss.db.validation.SourceValidator.TmpSource

class SourceUrlValidatorTests extends Specification with Mockito {

  import ZIOMaterializer._
  import SourceUrlValidator._
  import SourceValidatorTests._

  private val validHeaders = Map(contentTypeHeaderName -> "application/xml")

  "url validator" should {
    "validate url" should {
      "valid" in {
        val source = Gen.genNewSource.toTmp
        val validator = new SourceUrlValidator {
          override protected def makeRequest(url: String): Map[String, String] = {
            validHeaders
          }
        }
        validator.validateUrl(source) must beRight(source)
      }

      "invalid" in {
        val source = Gen.genNewSource.toTmp
        val validator = new SourceUrlValidator {
          override protected def makeRequest(url: String): Map[String, String] = {
            Map.empty
          }
        }
        validator.validateUrl(source) must beLeft(buildError(source.url))
      }

      "from exception" in {
        val source = Gen.genNewSource.toTmp
        val validator = new SourceUrlValidator {
          override protected def makeRequest(url: String): Map[String, String] = {
            throw new Exception("boom")
          }
        }
        validator.validateUrl(source) must beLeft(buildError(source.url))
      }
    }
  }

}
