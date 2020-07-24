package truerss.db.validation

import org.apache.commons.validator.routines.UrlValidator
import truerss.db.DbLayer
import truerss.dto.{ApplicationPlugins, NewSourceDto, SourceDto}
import truerss.services.ValidationError
import zio._
import zio.blocking._

// appPlugins is needed for custom content readers (non rss/atom)
class SourceValidator(private val dbLayer: DbLayer,
                      private val sourceUrlValidator: SourceUrlValidator,
                      private val appPlugins: ApplicationPlugins) {

  import SourceValidator._

  def filterValid(sources: Iterable[NewSourceDto]): Task[Iterable[NewSourceDto]] = {
    val urls = sources.map(_.url).toSeq
    val names = sources.map(_.name).toSeq

    for {
      notUnique <- dbLayer.sourceDao.findByUrlsAndNames(urls, names)
      notUniqueUrls = notUnique.map(_._1)
      notUniqueNames = notUnique.map(_._2)
      xs = sources
        .filter(isValidInterval)
        .filter(isValidUrl)
        .filterNot { x => notUniqueUrls.contains(x.url)   }
        .filterNot { x => notUniqueNames.contains(x.name) }
      (plugins, notPlugins) = xs.partition(isPlugin)
      validateUrls <- sourceUrlValidator.validateUrls(notPlugins.toSeq)
    } yield {
      plugins.toSeq ++ validateUrls.map(_.asInstanceOf[NewSourceDto])
    }
  }

  def validateSource(source: SourceDto): IO[ValidationError, SourceDto] = {
    val vInterval = validateInterval(source)
    val vUrl = validateUrl(source)
    val vUrlIsUnique = urlIsUnique(source)
    val vNameIsUnique = nameIsUnique(source)
    val vIsRss = validateRss(source)

    for {
      _ <- vInterval
      _ <- vUrl
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
  final val urlError = "Not valid url"
  def urlError(source: SourceDto) = s"Url '${source.url}' already present in db"
  def nameError(source: SourceDto) = s"Name '${source.name}' is not unique"

  def validateInterval(source: SourceDto): IO[ValidationError, SourceDto] = {
    if (isValidInterval(source)) {
      IO.effectTotal(source)
    } else {
      IO.fail(ValidationError(intervalError :: Nil))
    }
  }

  def isValidInterval(source: SourceDto): Boolean = {
    if (source.interval > 0) {
      true
    } else {
      false
    }
  }

  def validateUrl(source: SourceDto): IO[ValidationError, SourceDto] = {
    if (isValidUrl(source)) {
      IO.effectTotal(source)
    } else {
      IO.fail(ValidationError(urlError :: Nil))
    }
  }

  def isValidUrl(source: SourceDto): Boolean = {
    if (urlValidator.isValid(source.url)) {
      true
    } else {
      false
    }
  }

}