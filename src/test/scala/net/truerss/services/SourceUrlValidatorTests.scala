package net.truerss.services

import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import truerss.db.validation.SourceUrlValidator
import net.truerss.{Gen, ZIOMaterializer}

class SourceUrlValidatorTests extends Specification with Mockito {

  import ZIOMaterializer._
  import SourceUrlValidator._

  private val validHeaders = Map(contentTypeHeaderName -> "application/xml")

  "url validator" should {
    "validate url" should {
      "valid" in {
        val source = Gen.genNewSource
        val validator = new SourceUrlValidator {
          override protected def makeRequest(url: String): Map[String, String] = {
            validHeaders
          }
        }
        validator.validateUrl(source) must beRight(source)
      }

      "invalid" in {
        val source = Gen.genNewSource
        val validator = new SourceUrlValidator {
          override protected def makeRequest(url: String): Map[String, String] = {
            Map.empty
          }
        }
        validator.validateUrl(source) must beLeft(buildError(source.url))
      }

      "from exception" in {
        val source = Gen.genNewSource
        val validator = new SourceUrlValidator {
          override protected def makeRequest(url: String): Map[String, String] = {
            throw new Exception("boom")
          }
        }
        validator.validateUrl(source) must beLeft(buildError(source.url))
      }
    }

    "validate urls" should {
      "return only valid" in {
        val source1 = Gen.genNewSource
        val source2 = Gen.genNewSource
        val source3 = Gen.genNewSource
        val validator = new SourceUrlValidator {
          override protected def makeRequest(url: String): Map[String, String] = {
            url match {
              case source1.url => validHeaders
              case source2.url => Map.empty
              case _ => throw new Exception("boom")
            }
          }
        }
        validator.validateUrls(Seq(source1, source2, source3)).m ==== Seq(source1)
      }
    }
  }

}
