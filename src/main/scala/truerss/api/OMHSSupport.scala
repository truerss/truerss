package truerss.api

import com.github.fntz.omhs.{BodyReader, BodyWriter, CommonResponse}
import com.github.fntz.omhs.playjson.JsonSupport
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.util.CharsetUtil
import truerss.dto.{AvailableSetup, FeedContent, FeedDto, InstallPlugin, NewPluginSource, NewSetup, NewSourceDto, Page, PluginSourceDto, PluginsViewDto, Processing, SearchRequest, SourceOverview, SourceStatusDto, SourceViewDto, UninstallPlugin, UpdateSourceDto}

object OMHSSupport {
  import JsonFormats._

  implicit val pageWriter: BodyWriter[Page[FeedDto]] = JsonSupport.writer[Page[FeedDto]]()
  implicit val feedDtoWriter: BodyWriter[FeedDto] = JsonSupport.writer[FeedDto]()
  implicit val feedContentWriter: BodyWriter[FeedContent] = JsonSupport.writer[FeedContent]()

  implicit val c2c: BodyWriter[CommonResponse] = new BodyWriter[CommonResponse] {
    override def write(w: CommonResponse): CommonResponse = w
  }

  implicit val pluginsViewWriter: BodyWriter[PluginsViewDto] =
    JsonSupport.writer[PluginsViewDto]()

  implicit val searchRequestReader: BodyReader[SearchRequest] =
    JsonSupport.reader[SearchRequest]()

  type LNS = List[NewSetup[_]]

  implicit val currentSetupWriter: BodyWriter[Iterable[AvailableSetup[_]]] =
    JsonSupport.writer[Iterable[AvailableSetup[_]]]()

  implicit val newSetupReader: BodyReader[LNS] =
    JsonSupport.reader[LNS]()

  case class Setups(included: Iterable[NewSetup[_]])

  implicit lazy val setupsReader: BodyReader[Setups] = (str: String) => {
    Setups(newSetupReader.read(str))
  }

  implicit val sourceOverviewWriter: BodyWriter[SourceOverview] =
    JsonSupport.writer[SourceOverview]()

  implicit val sourceVViewWriter: BodyWriter[Vector[SourceViewDto]] =
    JsonSupport.writer[Vector[SourceViewDto]]()

  implicit val sourceViewWriter: BodyWriter[SourceViewDto] =
    JsonSupport.writer[SourceViewDto]()

  implicit val newSourceDtoReader: BodyReader[NewSourceDto] =
    JsonSupport.reader[NewSourceDto]()

  implicit val updateSourceDtoReader: BodyReader[UpdateSourceDto] =
    JsonSupport.reader[UpdateSourceDto]()

  implicit val processingWriter: BodyWriter[Processing] =
    JsonSupport.writer[Processing]()

  implicit val newPluginSourceReader: BodyReader[NewPluginSource] =
    JsonSupport.reader[NewPluginSource]()

  implicit val pluginSourceDtoWriter: BodyWriter[PluginSourceDto] =
    JsonSupport.writer[PluginSourceDto]()

  implicit val pluginSourceDtoIterableWriter: BodyWriter[Iterable[PluginSourceDto]] =
    JsonSupport.writer[Iterable[PluginSourceDto]]()

  implicit val installPluginReader: BodyReader[InstallPlugin] =
    JsonSupport.reader[InstallPlugin]()

  implicit val uninstallPluginReader: BodyReader[UninstallPlugin] =
    JsonSupport.reader[UninstallPlugin]()

  implicit val sourceStatusDtoWriter: BodyWriter[SourceStatusDto] =
    JsonSupport.writer[SourceStatusDto]()

  implicit val sourceStatusItDtoWriter: BodyWriter[Iterable[SourceStatusDto]] =
    JsonSupport.writer[Iterable[SourceStatusDto]]()

  case class Xml(text: String)
  implicit val xmlWriter: BodyWriter[Xml] = new BodyWriter[Xml] {
    override def write(w: Xml): CommonResponse = {
      CommonResponse(
        status = HttpResponseStatus.OK,
        contentType = "application/xml",
        content = w.text.getBytes(CharsetUtil.UTF_8)
      )
    }
  }

}
