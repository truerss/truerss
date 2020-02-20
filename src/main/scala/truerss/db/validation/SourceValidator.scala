package truerss.db.validation

import org.apache.commons.validator.routines.UrlValidator
import truerss.db.DbLayer
import truerss.dto.SourceDto
import truerss.util.{ApplicationPlugins, syntax}

import scala.concurrent.{ExecutionContext, Future}

// appPlugins is needed for custom content readers (non rss/atom)
class SourceValidator(appPlugins: ApplicationPlugins)(implicit dbLayer: DbLayer, ec: ExecutionContext) {

  import syntax.ext._

  type R = Either[String, SourceDto]
  type RL = Either[List[String], SourceDto]

  private val urlValidator = new UrlValidator()

  protected val sourceUrlValidator = new SourceUrlValidator()

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
    if (appPlugins.matchUrl(source.url)) {
      // plugin, skip rss/atom check
      source.right
    } else {
      sourceUrlValidator.validateUrl(source)
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
    if (source.interval > 0) {
      source.right
    } else {
      "Interval must be great than 0".left
    }
  }

  private def validateUrl(source: SourceDto): R = {
    if (urlValidator.isValid(source.url)) {
      source.right
    } else {
      "Not valid url".left
    }
  }

}
