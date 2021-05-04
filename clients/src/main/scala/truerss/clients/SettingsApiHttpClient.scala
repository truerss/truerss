package truerss.clients

import play.api.libs.json.Json
import truerss.dto.{AvailableSetup, NewSetup}
import zio.Task

class SettingsApiHttpClient(baseUrl: String) extends BaseHttpClient(baseUrl) {

  import JsonSupport._

  protected val settingsUrl = s"$baseUrl/$api/settings"

  def find: Task[Iterable[AvailableSetup[_]]] = {
    get[Iterable[AvailableSetup[_]]](s"$settingsUrl/current")
  }

  def update(newSetups: Iterable[NewSetup[_]]): Task[Unit] = {
    put[Unit](settingsUrl, Json.toJson(newSetups).toString())
  }

}
