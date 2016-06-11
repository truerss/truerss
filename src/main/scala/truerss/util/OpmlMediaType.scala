package truerss.util

import spray.http.MediaType
import spray.http.MediaTypes._

object OpmlMediaType {

  val opml = register(MediaType.custom(
    mainType = "text",
    subType = "x-opml-xml",
    compressible = true,
    binary = false,
    fileExtensions = Seq("opml")
  ))

}
