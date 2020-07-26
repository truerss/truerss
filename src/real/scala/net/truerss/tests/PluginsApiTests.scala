package net.truerss.tests

import net.truerss.{Resources, ZIOMaterializer}
import org.specs2.mutable.Specification
import truerss.clients.PluginsApiHttpClient
import truerss.dto.PluginsViewDto

trait PluginsApiTests extends Specification with Resources {

  import ZIOMaterializer._

  private val pluginHttpClient = new PluginsApiHttpClient(s"http://$host:$port")

  "plugins api" should {
    "current plugins" in {
      pluginHttpClient.getPlugins.m ==== PluginsViewDto()
    }

    "css" in {

      // TODO
//      pluginHttpClient.getCss.m ==== ""
      success
    }

    "js" in {
      success
      // TODO
      //pluginHttpClient.getJs.m ==== ""
    }
  }

}

