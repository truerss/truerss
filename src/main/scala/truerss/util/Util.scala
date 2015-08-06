package truerss.util

import truerss.models.Source
import org.apache.commons.validator.routines.UrlValidator
import scalaz._
import Scalaz._
import Scalaz.ToValidationOps
/**
 * Created by mike on 2.8.15.
 */
object Util {
  implicit class StringExt(s: String) {
    def normalize = s.replaceAll("[^\\p{L}\\p{Nd}]+", "-")
  }
}

trait Jsonize

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