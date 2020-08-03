package truerss.db.validation

import org.apache.commons.validator.routines.UrlValidator
import truerss.db.DbLayer
import truerss.db.driver.CurrentDriver
import truerss.dto.{ApplicationPlugins, NewSourceDto, SourceDto}
import truerss.services.ValidationError
import zio._

// appPlugins is needed for custom content readers (non rss/atom)
class SourceValidator(private val dbLayer: DbLayer,
                      private val sourceUrlValidator: SourceUrlValidator,
                      private val appPlugins: ApplicationPlugins) {

  import SourceValidator._

  def validateSource(source: SourceDto): IO[ValidationError, SourceDto] = {
    val vInterval = validateInterval(source)
    val vUrl = validateUrl(source)
    val vNameLength = validateNameLength(source)
    val vUrlLength =  validateUrlLength(source)
    val vUrlIsUnique = urlIsUnique(source)
    val vNameIsUnique = nameIsUnique(source)
    val vIsRss = validateRss(source)

    for {
      _ <- vInterval
      _ <- vUrl
      _ <- vNameLength
      _ <- vUrlLength
      _ <- vUrlIsUnique
      _ <- vNameIsUnique
      _ <- vIsRss
    } yield source
  }

  // skip validation if it's
  private def validateRss(source: SourceDto): IO[ValidationError, SourceDto] = {
    if (isPlugin(source)) {
      IO.effectTotal(source)
    } else {
      IO.fromEither(sourceUrlValidator.validateUrl(source))
        .mapError { err => ValidationError(err :: Nil) }
    }
  }

  private def isPlugin(source: SourceDto): Boolean = {
    appPlugins.matchUrl(source.url)
  }

  private def urlIsUnique(source: SourceDto): IO[ValidationError, Unit] = {
    for {
      count <- dbLayer.sourceDao.findByUrl(source.url, source.getId).orDie
      _ <- IO.fail(ValidationError(urlError(source) :: Nil)).when(count > 0)
    } yield ()
  }

  private def nameIsUnique(source: SourceDto): IO[ValidationError, Unit] = {
    for {
      count <- dbLayer.sourceDao.findByName(source.name, source.getId).orDie
      _ <- IO.fail(ValidationError(nameError(source) :: Nil)).when(count > 0)
    } yield ()
  }
}

object SourceValidator {

  private final val urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS)

  final val intervalError = "Interval must be great than 0"
  final val nameLengthError = s"Name length must be less than ${CurrentDriver.defaultLength} symbols"
  final val urlLengthError = s"Url length must be less than ${CurrentDriver.defaultLength} symbols"
  final val urlError = "Not valid url"
  def urlError(source: SourceDto) = s"Url '${source.url}' is not unique"
  def nameError(source: SourceDto) = s"Name '${source.name}' is not unique"

  def validateUrlLength(source: SourceDto): IO[ValidationError, SourceDto] = {
    if (isValidUrlLength(source)) {
      IO.succeed(source)
    } else {
      IO.fail(ValidationError(urlLengthError :: Nil))
    }
  }

  def isValidUrlLength(source: SourceDto): Boolean = {
    source.url.length <= CurrentDriver.defaultLength
  }

  def validateNameLength(source: SourceDto): IO[ValidationError, SourceDto] = {
    if (isValidNameLength(source)) {
      IO.succeed(source)
    } else {
      IO.fail(ValidationError(nameLengthError :: Nil))
    }
  }

  def isValidNameLength(source: SourceDto): Boolean = {
    source.name.length <= CurrentDriver.defaultLength
  }

  def validateInterval(source: SourceDto): IO[ValidationError, SourceDto] = {
    if (isValidInterval(source)) {
      IO.succeed(source)
    } else {
      IO.fail(ValidationError(intervalError :: Nil))
    }
  }

  def isValidInterval(source: SourceDto): Boolean = {
    source.interval > 0
  }

  def validateUrl(source: SourceDto): IO[ValidationError, SourceDto] = {
    if (isValidUrl(source)) {
      IO.succeed(source)
    } else {
      IO.fail(ValidationError(urlError :: Nil))
    }
  }

  def isValidUrl(source: SourceDto): Boolean = {
    urlValidator.isValid(source.url)
  }

}