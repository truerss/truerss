package truerss.services.management

import truerss.api.SettingsResponse
import truerss.services.SettingsService

import scala.concurrent.ExecutionContext

class SettingsManagement(val settingsService: SettingsService)
                        (implicit val ec: ExecutionContext) extends BaseManagement {

  def getCurrentSetup: R = {
    settingsService.getCurrentSetup.map(SettingsResponse(_))
  }

}
