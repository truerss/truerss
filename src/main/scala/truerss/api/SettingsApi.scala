package truerss.api

import akka.http.scaladsl.server.Directives._
import truerss.dto.{AvailableSetup, NewSetup}
import truerss.services.SettingsService

class SettingsApi(private val settingsService: SettingsService) extends HttpHelper {

  import JsonFormats._

  private val ss = settingsService

  val route = api {
    pathPrefix("settings") {
      get {
        pathPrefix("current") {
          w[Iterable[AvailableSetup[_]]](ss.getCurrentSetup)
        }
      } ~ put {
        createTR[Iterable[NewSetup[_]], Unit](x => ss.updateSetups(x))
      }
    }
  }

}
