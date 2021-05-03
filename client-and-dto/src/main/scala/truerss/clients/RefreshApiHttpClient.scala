package truerss.clients

import truerss.json.JsonFormats
import zio.Task

class RefreshApiHttpClient(baseUrl: String) extends BaseHttpClient(baseUrl) {

  import JsonFormats._

  protected val refreshUrl = s"$baseUrl/$api/refresh"

  def refreshAll: Task[Unit] = {
    put[Unit](s"$refreshUrl/all")
  }

  def refreshOne(sourceId: Long): Task[Unit] = {
    put[Unit](s"$refreshUrl/$sourceId")
  }

}
