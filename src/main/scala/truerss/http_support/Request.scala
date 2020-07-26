package truerss.http_support

import scalaj.http._
import zio.Task
import zio.blocking._

trait Request {

  import DefaultParameters._

  // TODO for settings
  protected val connectionTimeout = 10000
  protected val readTimeout = 10000
  protected val retryCount = 3

  def getResponseT(url: String): Task[HttpResponse[String]] = {
    effectBlockingIO(getResponse(url)).provideLayer(Blocking.live)
  }

  def getResponse(url: String): HttpResponse[String] = {
    handle(defaultRequest(url))
  }

  def getRequestHeaders(url: String): Map[String, String] = {
    handle(defaultRequest(url))
      .headers.map { x => x._1 -> x._2.mkString }
  }

  private def handle(req: HttpRequest): HttpResponse[String] = {
    runRequest(req, retryCount)
  }

  private def runRequest(req: HttpRequest, count: Int): HttpResponse[String] = {
    if (count == 0) {
      makeResponse(req)
    } else {
      val response = req.asString

      if (response.is3xx) {
        runRequest(defaultRequest(response.location.get), count - 1)
        handle(defaultRequest(response.location.get))
      } else {
        response
      }
    }
  }

  private def defaultRequest(url: String): HttpRequest = {
    scalaj.http.Http(url)
      .option(HttpOptions.connTimeout(connectionTimeout))
      .option(HttpOptions.readTimeout(readTimeout))
      .option(HttpOptions.allowUnsafeSSL)
      .option(HttpOptions.followRedirects(true))
      .header("Accept", "*/*")
      .compress(true)
      .header("User-Agent", userAgent)
  }

}

object DefaultParameters {

  val emptyResponse: HttpResponse[String] = new HttpResponse[String]("", code = 504, Map.empty)

  val userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1916.153 Safari/537.36"

  val userAgents = Vector(
    "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1",
    "Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10; rv:33.0) Gecko/20100101 Firefox/33.0",
    "Mozilla/5.0 (X11; Linux i586; rv:31.0) Gecko/20100101 Firefox/31.0",
    "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2227.1 Safari/537.36",
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2227.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2227.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2226.0 Safari/537.36",
    "Opera/9.80 (X11; Linux i686; Ubuntu/14.10) Presto/2.12.388 Version/12.16"
  )

  val makeResponse: HttpRequest => HttpResponse[String] = (req: HttpRequest) =>
    new HttpResponse[String](body = s"Can not handle request: ${req.url}", code = 504, Map.empty)
}

object Request extends Request

