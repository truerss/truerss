package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.dto.NewSetup
import truerss.services.management.SettingsManagement

import scala.concurrent.ExecutionContext

class SettingsApi(val settingsManagement: SettingsManagement)
                 (
                   implicit val ec: ExecutionContext
                 ) extends HttpHelper {

  import JsonFormats.newSetupFormat
  import ApiImplicits._

  private val sm = settingsManagement

  val route = api {
    pathPrefix("settings") {
      get {
        pathPrefix("current") {
          sm.getCurrentSetup
        }
      } ~ put {
        createT[Iterable[NewSetup[_]]](x => sm.updateSetups(x))
      }
    }
  }

}
