package truerss.api

import truerss.services.OpmlService
import truerss.util.OpmlExtractor
import zio.Task
import com.github.fntz.omhs.macros.RoutingImplicits
import com.github.fntz.omhs.playjson.JsonSupport
import com.github.fntz.omhs.{AsyncResult, BodyWriter, CommonResponse, ParamDSL}
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.multipart.MixedFileUpload
import io.netty.util.CharsetUtil

class OpmlApi(private val opmlService: OpmlService) extends HttpApi {

  import JsonFormats._
  import OpmlExtractor._
  import ParamDSL._
  import RoutingImplicits._
  import ZIOSupport._

  private implicit val processingWriter: BodyWriter[Processing] =
    JsonSupport.writer[Processing]

  case class Xml(text: String)
  private implicit val xmlWriter: BodyWriter[Xml] = new BodyWriter[Xml] {
    override def write(w: Xml): CommonResponse = {
      CommonResponse(
        status = HttpResponseStatus.OK,
        contentType = "application/xml",
        content = w.text.getBytes(CharsetUtil.UTF_8)
      )
    }
  }

  private val opml = get("api" / "v1" / "opml") ~> { () =>
    opmlService.build.map(Xml)
  }

  private val importFile = post("api" / "v1" / "import" / file) ~> { (fs: List[MixedFileUpload]) =>
    val content = fs.map(_.content().toString(CharsetUtil.UTF_8)).headOption.getOrElse("")
    reprocessToOpml(content)
    runImportAsync(content)
    Task(Processing()) // check status mb 201 ?
  }

  val route = opml :: importFile

  private def runImportAsync(text: String) = {
    zio.Runtime.default.unsafeRunTask(opmlService.create(text).forkDaemon)
  }

}

