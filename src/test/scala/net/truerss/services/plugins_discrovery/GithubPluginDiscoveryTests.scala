package net.truerss.services.plugins_discrovery

import org.specs2.mutable.Specification
import truerss.plugins_discrovery.GithubPluginDiscovery

class GithubPluginDiscoveryTests extends Specification {

  import GithubPluginDiscovery._

  "github" should {
    "isValid" in {
      isValidSource("https://github.com/truerss/plugins/releases/tag/1.0.0") must beTrue
      isValidSource("https://github.com/test/abc/releases/tag/1.0.0") must beTrue
      isValidSource("https://github.com/test/abc") must beFalse
      isValidSource("https://example.com/test/abc") must beFalse
    }
  }

}
