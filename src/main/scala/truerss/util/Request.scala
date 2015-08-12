package truerss.util

final object Request {

  import scalaj.http._

  // TODO for settings
  val connectionTimeout = 10000
  val readTimeout = 10000
  val userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.153 Safari/537.36"

  def getResponse(url: String): HttpResponse[String] = {
    val response = scalaj.http.Http(url)
      .option(HttpOptions.connTimeout(connectionTimeout))
      .option(HttpOptions.readTimeout(readTimeout))
      .option(HttpOptions.followRedirects(true))
      .compress(false)
      .header("User-Agent", userAgent).asString

    if (response.is3xx) {
      getResponse(response.location.get)
    } else {
      response
    }
  }

}
