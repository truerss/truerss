package truerss.services.management

import truerss.api.{BadRequestResponse, SettingsResponse}
import truerss.dto.NewSetup
import truerss.services.SettingsService

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class SettingsManagement(val settingsService: SettingsService)
                        (implicit val ec: ExecutionContext) extends BaseManagement {

  def getCurrentSetup: R = {
    settingsService.getCurrentSetup.map(SettingsResponse(_))
  }

  def updateSetup[T: ClassTag](newSetup: NewSetup[T]): R = {
    settingsService.getCurrentSetup.flatMap { xs =>
      if (xs.map(_.key).toVector.contains(newSetup.key)) {
        settingsService.updateSetup(newSetup).map { x =>
          logger.debug(s"Update setup: ${newSetup.key} with ${newSetup.value}, count: $x")
          ok
        }
      } else {
        Future.successful(BadRequestResponse(s"Unknown key: ${newSetup.key}"))
      }
    }
  }

}
