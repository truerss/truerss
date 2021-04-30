package truerss.plugins_discrovery

import zio.Task

trait DiscoveryProvider {
  def fetch(url: String): Task[Iterable[PluginJar]] = {
    Discovery.fetch(url)
  }
}


