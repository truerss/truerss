package truerss.api

import akka.stream.Materializer
import akka.http.scaladsl.server.Directives._
import truerss.dto.NewSetup
import truerss.services.management.SettingsManagement

import scala.concurrent.ExecutionContext

class SettingsApi(val settingsManagement: SettingsManagement)
                 (
                   implicit override val ec: ExecutionContext,
                   val materializer: Materializer
                 ) extends HttpHelper {

  import JsonFormats.newSetupFormat

  private val sm = settingsManagement

  val route = api {
    pathPrefix("settings") {
      get {
        pathPrefix("current") {
          call(sm.getCurrentSetup)
        }
      } ~ put {
        create[NewSetup[_]](x => sm.updateSetup(x))
      }
    }
  }

}
