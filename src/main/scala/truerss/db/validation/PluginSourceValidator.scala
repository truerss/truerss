package truerss.db.validation

import org.apache.commons.validator.routines.UrlValidator
import truerss.db.DbLayer
import truerss.dto.NewPluginSource
import truerss.services.ValidationError
import zio.IO

class PluginSourceValidator(private val dbLayer: DbLayer) {
  import PluginSourceValidator._

  def validate(newPluginSource: NewPluginSource): IO[ValidationError, NewPluginSource]  = {

    for {
      _ <- validateUrl(newPluginSource)
      _ <- validateUniqueUrl(newPluginSource)
    } yield newPluginSource

  }

  private def validateUniqueUrl(newPluginSource: NewPluginSource): IO[ValidationError, NewPluginSource] = {
    val url = newPluginSource.url
    for {
      count <- dbLayer.pluginSourcesDao.findByUrl(url).orDie
      _ <- IO.fail(ValidationError(urlError(url))).when(count > 0)
    } yield newPluginSource
  }

}

object PluginSourceValidator {
  private final val urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS)

  final val urlError = "Not valid url"
  def urlError(url: String) = s"Url '$url' is not unique"

  private def validateUrl(newPluginSource: NewPluginSource): IO[ValidationError, NewPluginSource] = {
    if (isValidUrl(newPluginSource.url)) {
      IO.succeed(newPluginSource)
    } else {
      IO.fail(ValidationError(urlError :: Nil))
    }
  }


  def isValidUrl(url: String): Boolean = {
    urlValidator.isValid(url)
  }
}