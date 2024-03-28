package truerss.db.validation

import org.apache.commons.validator.routines.UrlValidator
import truerss.db.DbLayer
import truerss.dto.NewPluginSource
import truerss.plugins_discrovery.GithubPluginDiscovery
import truerss.services.ValidationError
import zio.{IO, ZIO}

class PluginSourceValidator(private val dbLayer: DbLayer) {
  import PluginSourceValidator._

  def validate(newPluginSource: NewPluginSource): IO[ValidationError, NewPluginSource]  = {
    for {
      _ <- validateUrl(newPluginSource)
      _ <- isValidSourceUrl(newPluginSource)
      _ <- validateUniqueUrl(newPluginSource)
    } yield newPluginSource

  }

  private def validateUniqueUrl(newPluginSource: NewPluginSource): IO[ValidationError, NewPluginSource] = {
    val url = newPluginSource.url
    for {
      count <- dbLayer.pluginSourcesDao.findByUrl(url).orDie
      _ <- ZIO.fail(ValidationError(notUniqueUrlError(url))).when(count > 0)
    } yield newPluginSource
  }

}

object PluginSourceValidator {

  type R = IO[ValidationError, NewPluginSource]

  private val availableDiscoveries = Vector(
    GithubPluginDiscovery
  )
  private val availableSourceUrls = availableDiscoveries.map(_.url).mkString(", ")

  private final val urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS)

  final val urlError = "Not valid url"
  def notUniqueUrlError(url: String) = s"Url '$url' is not unique"
  def unknownSource(url: String) = s"Plugin source: '$url' is unknown, available: $availableSourceUrls"

  private def isValidSourceUrl(newPluginSource: NewPluginSource): R = {
    if (availableDiscoveries.exists(_.isValidSource(newPluginSource.url))) {
      ZIO.succeed(newPluginSource)
    } else {
      ZIO.fail(ValidationError(unknownSource(newPluginSource.url)))
    }
  }

  private def validateUrl(newPluginSource: NewPluginSource): R = {
    if (isValidUrl(newPluginSource.url)) {
      ZIO.succeed(newPluginSource)
    } else {
      ZIO.fail(ValidationError(urlError :: Nil))
    }
  }


  def isValidUrl(url: String): Boolean = {
    urlValidator.isValid(url)
  }
}