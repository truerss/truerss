package truerss.util

import org.apache.commons.validator.routines.UrlValidator
import shapeless._
import truerss.models.Source

object SourceValidator {

  import syntax.\/
  import syntax.ext._
  private val urlValidator = new UrlValidator()

  type R = String \/ Source

  def validate(source: Source): List[String] \/ Source = {
    validateInterval(source) :: validateUrl(source) :: HNil match {
      case Right(_) :: Right(_) :: HNil => source.right
      case Left(err) :: Right(_) :: HNil => l(err)
      case Right(_) :: Left(err) :: HNil => l(err)
      case Left(e1) :: Left(e2) :: HNil => l(e1, e2)
    }
  }

  private def l[T](x: T*) = List(x : _*).left

  private def validateInterval(source: Source): R = {
    if (source.interval > 0) {
      source.right
    } else {
      "Interval must be great than 0".left
    }
  }

  private def validateUrl(source: Source): R = {
    if (urlValidator.isValid(source.url)) {
      source.right
    } else {
      "Not valid url".left
    }
  }

}
