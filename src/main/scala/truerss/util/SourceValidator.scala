package truerss.util

import org.apache.commons.validator.routines.UrlValidator
import truerss.db.DbLayer
import truerss.dto.SourceDto

import scala.concurrent.{ExecutionContext, Future}

object SourceValidator {

  import syntax.ext._
  private val urlValidator = new UrlValidator()

  type R = Either[String, SourceDto]
  type RL = Either[List[String], SourceDto]

  def validateSource(source: SourceDto,
                     dbLayer: DbLayer)(
                      implicit ec: ExecutionContext
                    ): Future[RL] = {
    validate(source) match {
      case Right(_) =>
        for {
          urlIsUniq <- dbLayer.sourceDao.findByUrl(source.url, source.getId)
          nameIsUniq <- dbLayer.sourceDao.findByName(source.name, source.getId)
        } yield {
          (urlIsUniq, nameIsUniq) match {
            case (0, 0) =>
              source.right

            case (0, _) =>
              l(nameError(source))
            case (_, 0) =>
              l(urlError(source))
            case (_, _) =>
              l(urlError(source), nameError(source))
          }
        }

      case Left(errors) =>
        Future.successful(l(errors: _*))
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
