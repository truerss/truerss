package net.truerss.services

import org.specs2.mutable.Specification
import truerss.plugins_discrovery.Discovery
import truerss.util.TaskImplicits

class DiscoveryTests extends Specification {
  import TaskImplicits._

  private val url = "https://github.com/truerss/plugins/releases/tag/1.0.0"

  "fetch" in {
    Discovery.fetch(url).materialize must have size 4
  }

}
