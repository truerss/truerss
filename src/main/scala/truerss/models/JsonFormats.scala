package truerss.models

import java.util.Date

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
  implicit lazy val updateSourceDtoFormat = Json.format[UpdateSourceDto]
  implicit lazy val sourceDtoFormat = Json.format[SourceDto]
  implicit lazy val sourceViewDtoFormat = Json.format[SourceViewDto]

  implicit lazy val wsMessageFormat = Json.format[WSMessage]

  implicit lazy val pluginDtoFormat = Json.format[PluginDto]
  implicit lazy val pluginsViewDto = Json.format[PluginsViewDto]
  implicit lazy val feedDtoFormat = Json.format[FeedDto]

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


}
