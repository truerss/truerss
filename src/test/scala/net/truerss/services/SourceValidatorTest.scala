package net.truerss.services


import org.specs2.mutable.Specification
import truerss.util.SourceValidator

class SourceValidatorTest extends Specification {

  import Gen._

  "SourceValidator" should {
    "error when interval < 0" in {
      val source = genNewSource.copy(interval = 0)
      val result = SourceValidator.validate(source)
      result.isLeft must beTrue
      result match {
        case Right(r) => true must beFalse
        case Left(l) =>
          l ==== List("Interval must be great than 0")
      }
    }

    "error when not valid url" in {
      val source = genNewSource.copy(url = "abc")
      val result = SourceValidator.validate(source)
      result.isLeft must beTrue
      result match {
        case Right(r) => true must beFalse
        case Left(l) => l ==== List("Not valid url")
      }
    }

    "error when not valid url and interval" in {
      val source = genNewSource.copy(url = "abc", interval = 0)
      val result = SourceValidator.validate(source)
      result.isLeft should beTrue
      result match {
        case Right(r) => true must beFalse
        case Left(l) => l ==== List("Interval must be great than 0",
          "Not valid url")
      }
    }

    "when valid" in {
      val source = genNewSource
      val result = SourceValidator.validate(source)
      result.isRight must beTrue
      result match {
        case Right(r) => r ==== source
        case Left(l) => true should beFalse
      }
    }
  }

}
