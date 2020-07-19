package truerss.db.validation

import org.apache.commons.validator.routines.UrlValidator
import truerss.db.DbLayer
import truerss.db.validation.SourceValidatorZ.SourceValidatorZ
import truerss.dto.{ApplicationPlugins, NewSourceDto, SourceDto}
import truerss.services.ValidationError
import zio._

object SourceValidatorZ {

  type SourceValidatorZ = Has[Service]

  trait Service {
    def filterValid(sources: Iterable[NewSourceDto]): Task[Iterable[NewSourceDto]]
    def validateSource(source: SourceDto): IO[ValidationError, SourceDto]
  }

  final class Live(appPlugins: ApplicationPlugins)(
    implicit dbLayer: DbLayer
  ) extends Service {

    import SourceValidator._

    protected val sourceUrlValidator = new SourceUrlValidator()

    override def filterValid(sources: Iterable[NewSourceDto]): Task[Iterable[NewSourceDto]] = {
      val urls = sources.map(_.url).toSeq
      val names = sources.map(_.name).toSeq

      for {
        // TODO one method plz
        notUniqueUrls <- dbLayer.sourceDao.findByUrls(urls).orDie
        notUniqueNames <- dbLayer.sourceDao.findByNames(names)
        xs = sources
          .filter(isValidInterval)
          .filter(isValidUrl)
          .filterNot { x =>
            notUniqueUrls.contains(x.url)
          }.filterNot { x =>
          notUniqueNames.contains(x.name)
        }
        (plugins, notPlugins) = xs.partition(isPlugin)
        validateUrls <- sourceUrlValidator.validateUrls(notPlugins.toSeq)
      } yield {
        plugins.toSeq ++ validateUrls.map(_.asInstanceOf[NewSourceDto])
      }
    }

    override def validateSource(source: SourceDto): IO[ValidationError, SourceDto] = {
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

    private def urlIsUnique(source: SourceDto)(implicit dbLayer: DbLayer): IO[ValidationError, SourceDto] = {
      for {
        count <- dbLayer.sourceDao.findByUrl(source.url, source.getId).orDie
        _ <- IO.fail(ValidationError(urlError(source) :: Nil)).when(count > 0)

      } yield source
    }


    private def nameIsUnique(source: SourceDto)
                            (implicit dbLayer: DbLayer): IO[ValidationError, SourceDto] = {
      for {
        count <- dbLayer.sourceDao.findByName(source.url, source.getId).orDie
        _ <- IO.fail(ValidationError(nameError(source) :: Nil)).when(count > 0)
      } yield source
    }
  }

  def filterValid(sources: Iterable[NewSourceDto]): ZIO[SourceValidatorZ, Throwable, Iterable[NewSourceDto]] = {
    ZIO.accessM(_.get.filterValid(sources))
  }

  def validateSource(source: SourceDto): ZIO[SourceValidatorZ, ValidationError, SourceDto] = {
    ZIO.accessM(_.get.validateSource(source))
  }


}


// appPlugins is needed for custom content readers (non rss/atom)
class SourceValidator(appPlugins: ApplicationPlugins)(implicit dbLayer: DbLayer) {

  private val layer: Layer[Nothing, SourceValidatorZ] =
    ZLayer.succeed(new SourceValidatorZ.Live(appPlugins)(dbLayer))

  def filterValid(sources: Iterable[NewSourceDto]): Task[Iterable[NewSourceDto]] = {
    SourceValidatorZ.filterValid(sources).provideLayer(layer)
  }

  def validateSource(source: SourceDto): IO[ValidationError, SourceDto] = {
    SourceValidatorZ.validateSource(source).provideLayer(layer)
  }
}

object SourceValidator {

  private final val urlValidator = new UrlValidator()

  def validateInterval(source: SourceDto): IO[ValidationError, SourceDto] = {
    if (isValidInterval(source)) {
      IO.effectTotal(source)
    } else {
      IO.fail(ValidationError("Interval must be great than 0" :: Nil))
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
      IO.fail(ValidationError("Not valid url" :: Nil))
    }
  }

  def isValidUrl(source: SourceDto): Boolean = {
    if (urlValidator.isValid(source.url)) {
      true
    } else {
      false
    }
  }

  def urlError(source: SourceDto) = s"Url '${source.url}' already present in db"
  def nameError(source: SourceDto) = s"Name '${source.name}' is not unique"
}