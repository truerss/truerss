package truerss.api

import com.github.fntz.omhs.RoutingDSL
import truerss.dto.{InstallPlugin, NewPluginSource}
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

  private val deletePluginSource = delete(base / long) ~> {(id: Long) =>
    service.deletePluginSource(id)
  }

  private val install = post(base / "install" <<< body[InstallPlugin]) ~> {(p: InstallPlugin) =>
    service.installPlugin(p.url)
  }

  val route = all :: install :: deletePluginSource :: addNew

}
