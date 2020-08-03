package truerss.util

object OpmlExtractor {
  //
  // I use this method, because akka-http time to time does not run future inside a stream
  // from `fileUpload` directive
  //
  def reprocessToOpml(content: String): String = {
    content.split("\n")
      .filterNot(_.startsWith("---"))      // remove boundary
      .filterNot(_.startsWith("Content"))  // Content-* Headers
      .map(_.trim)
      .filterNot(_.isEmpty)
      .mkString("\n")
  }
}
