package truerss.util

import truerss.db.{SourceState, SourceStates}
import truerss.dto.ApplicationPlugins

object ApplicationPluginsImplicits {

  implicit class ApplicationPluginsExt(val a: ApplicationPlugins) extends AnyVal {
    def getState(url: String): SourceState = {
      if (a.matchUrl(url)) {
        SourceStates.Enable
      } else {
        SourceStates.Neutral
      }
    }
  }

}
