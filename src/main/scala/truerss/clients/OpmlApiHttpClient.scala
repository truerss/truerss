package truerss.clients

import scalaj.http.{Http, MultiPart}
import truerss.api.JsonFormats
import truerss.dto.SourceViewDto
import zio.Task

class OpmlApiHttpClient(baseUrl: String) extends BaseHttpClient(baseUrl) {

  import JsonFormats._

  protected val opmlUrl = s"$baseUrl/$api/opml"

  protected val defaultName = "import"
  protected val defaultFileName = "import"

  def importFile(opml: String): Task[Iterable[SourceViewDto]] = {
    handleRequest[Iterable[SourceViewDto]](
      Http(s"$opmlUrl/import").postMulti(
        MultiPart(defaultName, defaultFileName, "application/xml", opml)
      )
    )
  }

  def download: Task[String] = {
    rawGet(s"$baseUrl/opml")
  }

}
