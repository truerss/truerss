package truerss.models

import com.github.truerss.base.PluginInfo
import truerss.util.{ApplicationPlugins, Jsonize}
import truerss.system.util.Notify
import spray.json._

object ApiJsonProtocol extends DefaultJsonProtocol {
  import java.util.Date

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

  implicit object SourceFormat extends JsonFormat[Source] {
    //TODO possible validate url and name ?
    override def read(json: JsValue) = json.asJsObject
      .getFields("url", "name", "interval") match {
      case Seq(JsString(url), JsString(name), JsNumber(interval)) =>
        Source(
          id = None,
          url = url,
          name = name,
          interval = interval.toInt,
          state = Neutral,
          normalized = name,
          lastUpdate = new Date()
        )
      case _ => deserializationError("Not valid data")
    }

    override def write(s: Source) = {
      JsObject(
        "id" -> s.id.map(JsNumber(_)).getOrElse(JsNull),
        "url" -> JsString(s.url),
        "name" -> JsString(s.name),
        "interval" -> JsNumber(s.interval),
        "state" -> StateFormat.write(s.state),
        "normalized" -> JsString(s.normalized),
        "lastUpdate" -> DateFormat.write(s.lastUpdate),
        "count" -> JsNumber(s.count)
      )
    }

  }

  implicit val feedFormat = jsonFormat12(Feed)
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

  implicit def notifyLevelsWriter: JsonWriter[Notify] = new JsonWriter[Notify] {
    override def write(obj: Notify): JsValue = {
      JsObject("level" -> JsString(obj.level.name),
        "message" -> JsString(obj.message))
    }
  }

  implicit def jsonizeWriter: JsonWriter[Jsonize] = new JsonWriter[Jsonize] {
    def write(x: Jsonize) = x match {
      case x: Source => SourceFormat.write(x)
      //case x: SourceForFrontend => sourceForFrontendFormat.write(x)
      case x: Feed => feedFormat.write(x)
      case x: ApplicationPlugins => appPluginWriter.write(x)
    }
  }

  implicit def jsonizeVectorWriter: JsonWriter[Vector[Jsonize]] = new JsonWriter[Vector[Jsonize]] {
    override def write(xs: Vector[Jsonize]) = JsArray(xs.map(_.toJson))
  }





}
