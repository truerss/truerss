package truerss.api

import com.github.fntz.omhs.RoutingDSL
import truerss.services.SettingsService


class SettingsApi(private val settingsService: SettingsService) {

  import OMHSSupport._
  import RoutingDSL._
  import ZIOSupport._

  private val service = settingsService

  private val settings = get("api" / "v1" / "settings" / "current") ~> {() =>
    service.getCurrentSetup
  }

  private val updateSettings = put("api" / "v1" / "settings" <<< body[Setups]) ~> { (ns: Setups) =>
    service.updateSetups(ns.included)
  }

  val route = settings :: updateSettings


}
