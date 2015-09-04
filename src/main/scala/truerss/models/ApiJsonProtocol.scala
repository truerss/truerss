package truerss.models

import com.github.truerss.base.PluginInfo
import truerss.util.{ApplicationPlugins, Jsonize}
import spray.json._

object ApiJsonProtocol extends DefaultJsonProtocol {

  implicit object DateFormat extends JsonFormat[java.util.Date] {
    val format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    override def read(json: JsValue): java.util.Date =
      format.parse(json.convertTo[String])
    override def write(date: java.util.Date) =
      format.format(date).toJson
  }

  implicit object StateFormat extends JsonFormat[SourceState] {
    override def read(json: JsValue): SourceState = {
      json.convertTo[Byte] match {
        case 0 => Neutral
        case 1 => Enable
        case 2 => Disable
      }
    }
    override def write(s: SourceState) = s match {
      case Neutral => JsNumber(0)
      case Enable => JsNumber(1)
      case Disable => JsNumber(2)
    }
  }

  implicit val sourceFormat = jsonFormat8(Source)
  implicit val feedFormat = jsonFormat12(Feed)
  implicit val frontendSourceFormat = jsonFormat3(FrontendSource)
  implicit val sourceForFrontendFormat = jsonFormat8(SourceForFrontend)
  implicit val wsMessageFormat = jsonFormat2(WSMessage)

  implicit def appPluginWriter: JsonWriter[ApplicationPlugins] = new JsonWriter[ApplicationPlugins] {
    override def write(a: ApplicationPlugins) = {
      def info2js(x: PluginInfo) = JsObject(
        "author" -> JsString(x.author),
        "about" -> JsString(x.about),
        "version" -> JsString(x.version),
        "pluginName" -> JsString(x.pluginName)
      )
      JsObject("feed" -> JsArray(a.feedPlugins.map(info2js).toVector),
        "content" -> JsArray(a.contentPlugins.map(info2js).toVector),
        "publish" -> JsArray(a.publishPlugin.map(info2js).toVector),
        "site" -> JsArray(a.sitePlugin.map(info2js).toVector)
      )
    }
  }


  implicit def jsonizeWriter: JsonWriter[Jsonize] = new JsonWriter[Jsonize] {
    def write(x: Jsonize) = x match {
      case x: Source => sourceFormat.write(x)
      case x: SourceForFrontend => sourceForFrontendFormat.write(x)
      case x: Feed => feedFormat.write(x)
      case x: ApplicationPlugins => appPluginWriter.write(x)
    }
  }

  implicit def jsonizeVectorWriter: JsonWriter[Vector[Jsonize]] = new JsonWriter[Vector[Jsonize]] {
    override def write(xs: Vector[Jsonize]) = JsArray(xs.map(_.toJson))
  }





}
