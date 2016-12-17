package truerss.util

import org.apache.commons.validator.routines.UrlValidator
import truerss.models.Source
import scala.Either

object SourceValidator {

  import syntax.\/
  import syntax.ext._
  private val urlValidator = new UrlValidator()

  type R = Either[String, Source]
  type RL = Either[List[String], Source]

  def validate(source: Source): RL = {
    (validateInterval(source), validateUrl(source)) match {
      case (Right(_), Right(_)) => source.right
      case (Left(err), Right(_)) => l(err)
      case (Right(_), Left(err)) => l(err)
      case (Left(e1), Left(e2)) => l(e1, e2)
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
