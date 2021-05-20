package truerss.clients

import truerss.dto.SourceStatusDto
import zio.Task

class SourcesStatusesApiHttpClient(baseUrl: String) extends BaseHttpClient(baseUrl) {

  import JsonSupport._

  protected val apiUrl = s"$baseUrl/$api/source-statuses"

  def findAll: Task[Iterable[SourceStatusDto]] = {
    get[Iterable[SourceStatusDto]](s"$apiUrl/all")
  }

  def findOne(sourceId: Long): Task[SourceStatusDto] = {
    get[SourceStatusDto](s"$apiUrl/$sourceId")
  }

}
