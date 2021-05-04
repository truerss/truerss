package truerss.clients

import zio.Task

class MarkApiHttpClient(baseUrl: String) extends BaseHttpClient(baseUrl) {

  import JsonSupport._

  protected val markUrl = s"$baseUrl/$api/mark"

  def markAll: Task[Unit] = {
    put[Unit](markUrl)
  }

  def markOne(sourceId: Long): Task[Unit] = {
    put[Unit](s"$markUrl/$sourceId")
  }


}
