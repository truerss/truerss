
import truerss.models.Source
import truerss.util.SourceValidator
import org.specs2.mutable.Specification

class SourceValidatorTest extends Specification {

  import Gen._

  "SourceValidator" should {
    "error when interval < 0" in {
      val source = genSource().copy(interval = 0)
      val result = SourceValidator.validate(source)
      result.isLeft must beTrue
      result match {
        case Right(r) => true must beFalse
        case Left(l) =>
          l ==== List("Interval must be great than 0")
      }
    }

    "error when not valid url" in {
      val source = genSource().copy(url = "abc")
      val result = SourceValidator.validate(source)
      result.isLeft must beTrue
      result match {
        case Right(r) => true must beFalse
        case Left(l) => l ==== List("Not valid url")
      }
    }

    "error when not valid url and interval" in {
      val source = genSource().copy(url = "abc", interval = 0)
      val result = SourceValidator.validate(source)
      result.isLeft should beTrue
      result match {
        case Right(r) => true must beFalse
        case Left(l) => l ==== List("Interval must be great than 0",
          "Not valid url")
      }
    }

    "when valid" in {
      val source = genSource()
      val result = SourceValidator.validate(source)
      result.isRight must beTrue
      result match {
        case Right(r) => r ==== source
        case Left(l) => true should beFalse
      }
    }
  }

}
