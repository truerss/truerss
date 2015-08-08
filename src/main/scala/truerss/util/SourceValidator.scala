package truerss.util

import org.apache.commons.validator.routines.UrlValidator
import truerss.models.Source

import scalaz.Scalaz.{ToValidationOps, _}
import scalaz._

/**
 * Created by mike on 8.8.15.
 */
object SourceValidator {

  private val urlValidator = new UrlValidator()
  type V =  ValidationNel[String, Source]
  def validate(source: Source) = {
    (validateInterval(source) |@| validateUrl(source)) {(a, b) => a}.toEither
  }

  private def validateInterval(source: Source) = {
    if (source.interval > 0) {
      source.successNel
    } else {
      "Interval must be great than 0".failureNel
    }
  }

  private def validateUrl(source: Source) = {
    if (urlValidator.isValid(source.url)) {
      source.successNel
    } else {
      "Not valid url".failureNel
    }
  }

}
