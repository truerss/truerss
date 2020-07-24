package truerss.clients

import scalaj.http.Http
import truerss.api.JsonFormats
import truerss.dto.PluginsViewDto
import zio.Task

class PluginsApiHttpClient(baseUrl: String) extends BaseHttpClient(baseUrl) {

  import JsonFormats._

  protected val pluginsUrl = s"$baseUrl/$api/plugins"

  def getPlugins: Task[PluginsViewDto] = {
    handleRequest[PluginsViewDto](Http(s"$pluginsUrl/all").method("GET"))
  }

  def getCss: Task[String] = {
    handleRequest[String](Http(s"$pluginsUrl/css").method("GET"))
  }

  def getJs: Task[String] = {
    handleRequest[String](Http(s"$pluginsUrl/js").method("GET"))
  }

}

