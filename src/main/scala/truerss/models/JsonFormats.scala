package truerss.models

import java.util.Date

import com.github.truerss.base.PluginInfo
import truerss.util.{ApplicationPlugins, Jsonize}
import play.api.libs.json._
import truerss.dto._

object JsonFormats {

  def J[T: Writes](x: T): String = Json.stringify(Json.toJson(x))

  implicit class StringExtJson(val x: String) extends AnyVal {
    def j: JsString = JsString(x)
  }

  implicit object DateFormat extends Format[Date] {
    private final val f = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    override def reads(json: JsValue): JsResult[Date] = {
      json match {
        case JsString(value) =>
          JsSuccess(f.parse(value))
        case _ =>
          JsError("String required")
      }
    }

    override def writes(o: Date): JsValue = {
      JsString(f.format(o))
    }
  }

  implicit object StateFormat extends Format[SourceState] {
    override def reads(json: JsValue): JsResult[SourceState] = {
      json match {
        case JsNumber(value) if value == Neutral.number =>
          JsSuccess(Neutral)

        case JsNumber(value) if value == Enable.number =>
          JsSuccess(Enable)

        case JsNumber(value) if value == Disable.number =>
          JsSuccess(Disable)

        case JsNumber(value) =>
          JsError("Invalid state")

        case _ =>
          JsError("Number required")
      }
    }

    override def writes(o: SourceState): JsValue = {
      JsNumber(o.number)
    }
  }

  implicit lazy val newSourceDtoFormat = Json.format[NewSourceDto]
  implicit lazy val sourceWFormat = Json.format[SourceW]
  implicit lazy val updateSourceDtoFormat = Json.format[UpdateSourceDto]
  implicit lazy val sourceDtoFormat = Json.format[SourceDto]
  implicit lazy val sourceFormat = Json.format[Source]
  implicit lazy val sourceViewDtoFormat = Json.format[SourceViewDto]

  implicit lazy val feedFormat = Json.format[Feed]
  implicit lazy val wsMessageFormat = Json.format[WSMessage]

  implicit lazy val appPluginsWrites: Writes[ApplicationPlugins] = new Writes[ApplicationPlugins] {
    private val pluginInfoFormat: Writes[PluginInfo] = new Writes[PluginInfo] {
      override def writes(o: PluginInfo): JsValue = {
        JsObject(
          Seq(
            "author" -> o.author.j,
            "about" -> o.about.j,
            "version" -> o.version.j,
            "pluginName" -> o.pluginName.j
          )
        )
      }
    }
    override def writes(o: ApplicationPlugins): JsValue = {
      JsObject(
        Seq(
          "feed" -> JsArray(o.feedPlugins.map(pluginInfoFormat.writes)),
          "content" -> JsArray(o.contentPlugins.map(pluginInfoFormat.writes)),
          "publish" -> JsArray(o.publishPlugins.map(pluginInfoFormat.writes)),
          "site" -> JsArray(o.sitePlugins.map(pluginInfoFormat.writes))
        )
      )
    }
  }

  implicit lazy val pluginDtoFormat = Json.format[PluginDto]
  implicit lazy val pluginsViewDto = Json.format[PluginsViewDto]

  implicit val notifyLevelWrites: Writes[Notify] = new Writes[Notify] {
    override def writes(o: Notify): JsValue = {
      JsObject(
        Seq(
          "level" -> o.level.name.j,
          "message" -> o.message.j
        )
      )
    }
  }

  object JsonizeJ {
    implicit val jsonizeWrites: Writes[Jsonize] = new Writes[Jsonize] {
      override def writes(o: Jsonize): JsValue = {
        o match {
          case x: Source => sourceFormat.writes(x)
          case x: Feed => feedFormat.writes(x)
          case x: ApplicationPlugins => appPluginsWrites.writes(x)
        }
      }
    }
  }


  /*

  implicit def jsonizeVectorWriter: JsonWriter[Vector[Jsonize]] = new JsonWriter[Vector[Jsonize]] {
    override def write(xs: Vector[Jsonize]) = JsArray(xs.map(_.toJson))
  }

   */


}
