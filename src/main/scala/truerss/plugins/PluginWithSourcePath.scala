package truerss.plugins

import com.github.truerss.base.PluginInfo

case class PluginWithSourcePath[T <: PluginInfo](
                                                  plugin: T,
                                                  jarSourcePath: String
                                                )
