package truerss.dto

case class NewPluginSource(url: String)

// urls
case class PluginSourceDto(id: Long, url: String, plugins: Iterable[String])

case class InstallPlugin(url: String)