package truerss.api

import com.github.fntz.omhs.RoutingDSL
import truerss.dto.NewPluginSource
import truerss.services.PluginSourcesService

class PluginSourcesApi(private val service: PluginSourcesService) {

  import RoutingDSL._
  import OMHSSupport._
  import ZIOSupport._

  private val base = "api" / "v1" / "plugin-sources"

  private val all = get(base) ~> { () =>
    service.availablePluginSources
  }

  private val addNew = post(base <<< body[NewPluginSource]) ~> { (p: NewPluginSource) =>
    service.addNew(p)
  }

  val route = all :: addNew

}
