package net.truerss.tests

import net.truerss.Resources
import org.specs2.mutable.Specification

trait AllTestsTogether extends Specification
  with PluginsApiTests
  with SettingsApiTests
  with FullFlowTests { self: Resources =>

}
