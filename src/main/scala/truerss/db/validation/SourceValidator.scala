package truerss.db.validation

import org.apache.commons.validator.routines.UrlValidator
import truerss.db.DbLayer
import truerss.db.driver.CurrentDriver
import truerss.dto.{NewSourceDto, UpdateSourceDto}
import truerss.services.{ApplicationPluginsService, ValidationError}
import zio._

// appPlugins is needed for custom content readers (non rss/atom)
class SourceValidator(private val dbLayer: DbLayer,
                      private val sourceUrlValidator: SourceUrlValidator,
                      private val appPluginsService: ApplicationPluginsService) {

  import SourceValidator._

  def validateSource(source: NewSourceDto): IO[ValidationError, NewSourceDto] = {
    val tmp = TmpSource(
      id = None,
      name = source.name,
      url = source.url,
      interval = source.interval
    )
    validateSource(tmp).map { _ => source }
  }

  def validateSource(source: UpdateSourceDto): IO[ValidationError, UpdateSourceDto] = {
    val tmp = TmpSource(
      id = Some(source.id),
      name = source.name,
      url = source.url,
      interval = source.interval
    )
    validateSource(tmp).map { _ => source }
  }

  private def validateSource(source: TmpSource): IO[ValidationError, TmpSource] = {
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
  private def validateRss(source: TmpSource): IO[ValidationError, TmpSource] = {
    if (isPlugin(source)) {
      IO.effectTotal(source)
    } else {
      IO.fromEither(sourceUrlValidator.validateUrl(source))
        .mapError { err => ValidationError(err :: Nil) }
    }
  }

  private def isPlugin(source: TmpSource): Boolean = {
    appPluginsService.matchUrl(source.url)
  }

  private def urlIsUnique(source: TmpSource): IO[ValidationError, Unit] = {
    for {
      count <- dbLayer.sourceDao.findByUrl(source.url, source.id).orDie
      _ <- IO.fail(ValidationError(urlError(source) :: Nil)).when(count > 0)
    } yield ()
  }

  private def nameIsUnique(source: TmpSource): IO[ValidationError, Unit] = {
    for {
      count <- dbLayer.sourceDao.findByName(source.name, source.id).orDie
      _ <- IO.fail(ValidationError(nameError(source) :: Nil)).when(count > 0)
    } yield ()
  }
}

object SourceValidator {

  case class TmpSource(id: Option[Long], name: String, url: String, interval: Int)

  private final val urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS)

  final val intervalError = "Interval must be great than 0"
  final val nameLengthError = s"Name length must be less than ${CurrentDriver.defaultLength} symbols"
  final val urlLengthError = s"Url length must be less than ${CurrentDriver.defaultLength} symbols"
  final val urlError = "Not valid url"
  def urlError(source: TmpSource) = s"Url '${source.url}' is not unique"
  def nameError(source: TmpSource) = s"Name '${source.name}' is not unique"

  def validateUrlLength(source: TmpSource): IO[ValidationError, TmpSource] = {
    if (isValidUrlLength(source)) {
      IO.succeed(source)
    } else {
      IO.fail(ValidationError(urlLengthError :: Nil))
    }
  }

  def isValidUrlLength(source: TmpSource): Boolean = {
    source.url.length <= CurrentDriver.defaultLength
  }

  def validateNameLength(source: TmpSource): IO[ValidationError, TmpSource] = {
    if (isValidNameLength(source)) {
      IO.succeed(source)
    } else {
      IO.fail(ValidationError(nameLengthError :: Nil))
    }
  }

  def isValidNameLength(source: TmpSource): Boolean = {
    source.name.length <= CurrentDriver.defaultLength
  }

  def validateInterval(source: TmpSource): IO[ValidationError, TmpSource] = {
    if (isValidInterval(source)) {
      IO.succeed(source)
    } else {
      IO.fail(ValidationError(intervalError :: Nil))
    }
  }

  def isValidInterval(source: TmpSource): Boolean = {
    source.interval > 0
  }

  def validateUrl(source: TmpSource): IO[ValidationError, TmpSource] = {
    if (isValidUrl(source)) {
      IO.succeed(source)
    } else {
      IO.fail(ValidationError(urlError :: Nil))
    }
  }

  def isValidUrl(source: TmpSource): Boolean = {
    urlValidator.isValid(source.url)
  }

}