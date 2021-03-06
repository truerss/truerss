package truerss.clients

import scalaj.http.{Http, MultiPart}
import zio.Task

class OpmlApiHttpClient(baseUrl: String) extends BaseHttpClient(baseUrl) {

  import JsonSupport._

  protected val opmlUrl = s"$baseUrl/$api/opml"

  protected val defaultName = "import"
  protected val defaultFileName = "import"

  def importFile(opml: String): Task[Unit] = {
    handleRequest[Unit](
      Http(s"$opmlUrl/import").postMulti(
        MultiPart(defaultName, defaultFileName, "application/xml", opml)
      )
    )
  }

  def download: Task[String] = {
    rawGet(opmlUrl)
  }

}
