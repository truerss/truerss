package truerss.db.validation

import org.apache.commons.validator.routines.UrlValidator
import truerss.db.DbLayer
import truerss.dto.{ApplicationPlugins, NewSourceDto, SourceDto}
import truerss.services.ValidationError
import truerss.util.syntax
import zio._

import scala.concurrent.{ExecutionContext, Future}


// appPlugins is needed for custom content readers (non rss/atom)
class SourceValidator(appPlugins: ApplicationPlugins)(implicit dbLayer: DbLayer, ec: ExecutionContext) {

  private val urlValidator = new UrlValidator()

  protected val sourceUrlValidator = new SourceUrlValidator()

  def validateSources(sources: Iterable[NewSourceDto]): Future[Seq[NewSourceDto]] = {
    val urls = sources.map(_.url).toSeq
    val names = sources.map(_.name).toSeq

    val notUniqUrlsF = dbLayer.sourceDao.findByUrls(urls)
    val notUniqNamesF = dbLayer.sourceDao.findByNames(names)
    for {
      notUniqUrls <- notUniqUrlsF
      notUniqNames <- notUniqNamesF
      xs = sources
        .filter(isValidInterval)
        .filter(isValidUrl)
        .filterNot { x =>
          notUniqUrls.contains(x.url)
        }.filterNot { x =>
        notUniqNames.contains(x.name)
      }
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
    if (appPlugins.matchUrl(source.url)) {
      // , skip rss/atom check
      true
    } else {
      false
    }
  }

  private def urlIsUnique(source: SourceDto)(implicit dbLayer: DbLayer,
                                              ec: ExecutionContext): IO[ValidationError, SourceDto] = {
    for {
      count <- dbLayer.sourceDao.findByUrl1(source.url, source.getId).orDie
      _ <- IO.fail(ValidationError(urlError(source) :: Nil)).when(count > 0)

    } yield source
  }


  private def nameIsUnique(source: SourceDto)
                          (implicit dbLayer: DbLayer,
                           ec: ExecutionContext): IO[ValidationError, SourceDto] = {
    for {
      count <- dbLayer.sourceDao.findByName1(source.url, source.getId).orDie
      _ <- IO.fail(ValidationError(nameError(source) :: Nil)).when(count > 0)
    } yield source
  }

  private def validateInterval(source: SourceDto): IO[ValidationError, SourceDto] = {
    if (isValidInterval(source)) {
      IO.effectTotal(source)
    } else {
      IO.fail(ValidationError("Interval must be great than 0" :: Nil))
    }
  }

  private def isValidInterval(source: SourceDto): Boolean = {
    if (source.interval > 0) {
      true
    } else {
      false
    }
  }

  private def validateUrl(source: SourceDto): IO[ValidationError, SourceDto] = {
    if (isValidUrl(source)) {
      IO.effectTotal(source)
    } else {
      IO.fail(ValidationError("Not valid url" :: Nil))
    }
  }

  private def isValidUrl(source: SourceDto): Boolean = {
    if (urlValidator.isValid(source.url)) {
      true
    } else {
      false
    }
  }

  private def urlError(source: SourceDto) = s"Url '${source.url}' already present in db"
  private def nameError(source: SourceDto) = s"Name '${source.name}' is not unique"

}
