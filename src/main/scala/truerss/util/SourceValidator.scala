package truerss.util

import org.apache.commons.validator.routines.UrlValidator
import shapeless._
import truerss.models.Source

object SourceValidator {

  private val urlValidator = new UrlValidator()

  type R = Either[String, Source]

  def validate(source: Source): Either[List[String], Source] = {
    validateInterval(source) :: validateUrl(source) :: HNil match {
      case Right(_) :: Right(_) :: HNil => Right(source)
      case Left(err) :: Right(_) :: HNil => Left(List(err))
      case Right(_) :: Left(err) :: HNil => Left(List(err))
      case Left(e1) :: Left(e2) :: HNil => Left(List(e1, e2))
    }
  }

  private def validateInterval(source: Source): R = {
    if (source.interval > 0) {
      Right(source)
    } else {
      Left("Interval must be great than 0")
    }
  }

  private def validateUrl(source: Source): R = {
    if (urlValidator.isValid(source.url)) {
      Right(source)
    } else {
      Left("Not valid url")
    }
  }

}
