package truerss.db.validation

import org.apache.commons.validator.routines.UrlValidator
import truerss.db.DbLayer
import truerss.dto.{NewSourceDto, SourceDto}
import truerss.util.{ApplicationPlugins, syntax}

import scala.concurrent.{ExecutionContext, Future}

// appPlugins is needed for custom content readers (non rss/atom)
class SourceValidator(appPlugins: ApplicationPlugins)(implicit dbLayer: DbLayer, ec: ExecutionContext) {

  import syntax.ext._

  type R = Either[String, SourceDto]
  type RL = Either[List[String], SourceDto]
  type RLI = Either[List[String], Iterable[SourceDto]]

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

  def validateSource(source: SourceDto): Future[RL] = {
    val vInterval = Future.successful(validateInterval(source))
    val vUrl = Future.successful(validateUrl(source))
    val vUrlIsUnique = urlIsUnique(source)
    val vNameIsUnique = nameIsUnique(source)
    val vIsRss = Future { validateRss(source) }

    Future.sequence(
      Seq(vInterval, vUrl, vUrlIsUnique, vNameIsUnique, vIsRss)
    ).map { results =>
      val (errors, _) = results.partition(_.isLeft)
      if (errors.nonEmpty) {
        errors.map(_.swap).flatMap(_.toOption).toList.left
      } else {
        source.right
      }
    }
  }

  def validate(source: SourceDto): RL = {
    (validateInterval(source), validateUrl(source)) match {
      case (Right(_), Right(_)) => source.right
      case (Left(err), Right(_)) => l(err)
      case (Right(_), Left(err)) => l(err)
      case (Left(e1), Left(e2)) => l(e1, e2)
    }
  }

  private def urlError(source: SourceDto) = s"Url '${source.url}' already present in db"
  private def nameError(source: SourceDto) = s"Name '${source.name}' is not unique"

  private def l[T](x: T*) = List(x : _*).left

  // skip validation if it's
  private def validateRss(source: SourceDto): R = {
    if (isPlugin(source)) {
      // plugin, skip rss/atom check
      source.right
    } else {
      sourceUrlValidator.validateUrl(source)
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

  private def urlIsUnique(source: SourceDto)(implicit dbLayer: DbLayer, ec: ExecutionContext) = {
    dbLayer.sourceDao.findByUrl(source.url, source.getId).map { x =>
      if (x > 0) {
        urlError(source).left
      } else {
        source.right
      }
    }
  }

  private def nameIsUnique(source: SourceDto)(implicit dbLayer: DbLayer, ec: ExecutionContext) = {
    dbLayer.sourceDao.findByName(source.url, source.getId).map { x =>
      if (x > 0) {
        nameError(source).left
      } else {
        source.right
      }
    }
  }

  private def validateInterval(source: SourceDto): R = {
    if (isValidInterval(source)) {
      source.right
    } else {
      "Interval must be great than 0".left
    }
  }

  private def isValidInterval(source: SourceDto): Boolean = {
    if (source.interval > 0) {
      true
    } else {
      false
    }
  }

  private def validateUrl(source: SourceDto): R = {
    if (isValidUrl(source)) {
      source.right
    } else {
      "Not valid url".left
    }
  }

  private def isValidUrl(source: SourceDto): Boolean = {
    if (urlValidator.isValid(source.url)) {
      true
    } else {
      false
    }
  }

}
