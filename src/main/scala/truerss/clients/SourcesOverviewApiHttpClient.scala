package truerss.clients

import truerss.api.JsonFormats
import truerss.dto.SourceOverview
import zio.Task

class SourcesOverviewApiHttpClient(baseUrl: String) extends BaseHttpClient(baseUrl) {

  import JsonFormats._

  def overview(sourceId: Long): Task[SourceOverview] = {
    get[SourceOverview](s"$baseUrl/$api/overview/$sourceId")
  }


}
