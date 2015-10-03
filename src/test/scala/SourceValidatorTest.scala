
import truerss.models.Source
import truerss.util.SourceValidator
import org.scalatest.{Matchers, FunSpec}

class SourceValidatorTest extends FunSpec with Matchers {

  import Gen._

  describe("SourceValidator") {
    it("error when interval < 0") {
      val source = genSource().copy(interval = 0)
      val result = SourceValidator.validate(source)
      result.isLeft should be(true)
      result match {
        case Right(r) => true should be(false)
        case Left(l) => l.toList should be(List("Interval must be great than 0"))
      }
    }

    it("error when not valid url") {
      val source = genSource().copy(url = "abc")
      val result = SourceValidator.validate(source)
      result.isLeft should be(true)
      result match {
        case Right(r) => true should be(false)
        case Left(l) => l.toList should be(List("Not valid url"))
      }
    }

    it("error when not valid url and interval") {
      val source = genSource().copy(url = "abc", interval = 0)
      val result = SourceValidator.validate(source)
      result.isLeft should be(true)
      result match {
        case Right(r) => true should be(false)
        case Left(l) => l.toList should be(List("Interval must be great than 0",
          "Not valid url"))
      }
    }

    it("when valid") {
      val source = genSource()
      val result = SourceValidator.validate(source)
      result.isRight should be(true)
      result match {
        case Right(r) => r should be(source)
        case Left(l) => true should be(false)
      }
    }

  }
}
