package truerss.api

import com.github.fntz.omhs.RoutingDSL
import io.netty.handler.codec.http.multipart.FileUpload
import io.netty.util.CharsetUtil
import truerss.services.OpmlService
import truerss.util.OpmlExtractor
import zio.Task

class OpmlApi(private val opmlService: OpmlService) {

  import OMHSSupport._
  import OpmlExtractor._
  import RoutingDSL._
  import ZIOSupport._

  private val opml = get("api" / "v1" / "opml") ~> { () =>
    opmlService.build.map(Xml)
  }

  private val rawOpml = get("opml") ~> {() =>
    opmlService.build.map(Xml)
  }

  private val importFile = post("api" / "v1" / "opml" / "import" <<< file("import")) ~> { (fs: List[FileUpload]) =>
    val content = fs.map(_.content().toString(CharsetUtil.UTF_8)).headOption.getOrElse("")
    reprocessToOpml(content)
    runImportAsync(content)
    Task.unit
  }

  val route = opml :: importFile :: rawOpml

  private def runImportAsync(text: String) = {
    zio.Runtime.default.unsafeRunTask(opmlService.create(text).forkDaemon)
  }

}

