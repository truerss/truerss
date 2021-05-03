package truerss.clients

import truerss.dto.SourceOverview
import truerss.json.JsonFormats
import zio.Task

class SourcesOverviewApiHttpClient(baseUrl: String) extends BaseHttpClient(baseUrl) {

  import JsonFormats._

  def overview(sourceId: Long): Task[SourceOverview] = {
    get[SourceOverview](s"$baseUrl/$api/overview/$sourceId")
  }


}
