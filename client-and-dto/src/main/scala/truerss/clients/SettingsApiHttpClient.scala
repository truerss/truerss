package truerss.clients

import play.api.libs.json.Json
import truerss.dto.{AvailableSetup, NewSetup}
import truerss.json.JsonFormats
import zio.Task

class SettingsApiHttpClient(baseUrl: String) extends BaseHttpClient(baseUrl) {

  import JsonFormats._

  protected val settingsUrl = s"$baseUrl/$api/settings"

  def find: Task[Iterable[AvailableSetup[_]]] = {
    get[Iterable[AvailableSetup[_]]](s"$settingsUrl/current")
  }

  def update(newSetups: Iterable[NewSetup[_]]): Task[Unit] = {
    put[Unit](settingsUrl, Json.toJson(newSetups).toString())
  }

}
