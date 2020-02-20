package truerss.api

import java.util.Date

import play.api.libs.json._
import truerss.db._
import truerss.db.driver.{CheckBoxValue, Settings, ReadContent, SelectableValue, SettingKey, SettingValue, UnknownKey}
import truerss.dto.{Notify, _}

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
        case JsNumber(value) if value == SourceStates.Neutral.number =>
          JsSuccess(SourceStates.Neutral)

        case JsNumber(value) if value == SourceStates.Enable.number =>
          JsSuccess(SourceStates.Enable)

        case JsNumber(value) if value == SourceStates.Disable.number =>
          JsSuccess(SourceStates.Disable)

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

  implicit lazy val newSourceFromFile = Json.format[NewSourceFromFileWithErrors]

  implicit lazy val sourceImportResult: Writes[Map[Int, Either[NewSourceFromFileWithErrors, SourceViewDto]]] = new Writes[Map[Int, Either[NewSourceFromFileWithErrors, SourceViewDto]]] {
    override def writes(o: Map[Int, Either[NewSourceFromFileWithErrors, SourceViewDto]]): JsValue = {
      val r = o.map { case (k, v) =>
        val tmp = v match {
          case Left(x) => newSourceFromFile.writes(x)
          case Right(x) => sourceViewDtoFormat.writes(x)
        }
        k.toString -> tmp
      }

      JsObject(r)
    }
  }

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

  implicit lazy val settingKeyFormat: Format[SettingKey] = new Format[SettingKey] {
    override def writes(o: SettingKey): JsValue = {
      JsString(o.name)
    }

    override def reads(json: JsValue): JsResult[SettingKey] = {
      json match {
        case JsString(str) =>
          val key = str match {
            case ReadContent.name => ReadContent
            case x => UnknownKey(x)
          }
          JsSuccess(key)
        case x =>
          JsError(s"Unexpected type: $x")
      }
    }
  }

  implicit lazy val settingValueFormat: Format[SettingValue] = new Format[SettingValue] {
    private val fType = "type"
    private val fValues = "values"
    private val fValue = "value"

    override def writes(o: SettingValue): JsValue = {
      o match {
        case i @ SelectableValue(xs) =>
          JsObject(
            Seq(
              fType -> JsString(i.name),
              fValues -> JsArray(xs.map(x => JsString(x)).toSeq)
            )
          )
        case i @ CheckBoxValue(currentState) =>
          JsObject(
            Seq(
              fType -> JsString(i.name),
              fValue -> JsBoolean(currentState)
            )
          )
      }
    }

    override def reads(json: JsValue): JsResult[SettingValue] = {
      json match {
        case JsObject(obj) =>
          val tpe = obj.get(fType)
          tpe match {
            case Some(JsString(SelectableValue.fName)) =>
              val values = obj.get(fValues)
                .collect { case xs: JsArray => xs }
                .map { arr => arr.value.collect { case JsString(value) => value } }
                .getOrElse(Iterable.empty)
              JsSuccess(
                SelectableValue(
                  values
                )
              )

            case Some(JsString(CheckBoxValue.fName)) =>
              val state = obj.get(fValue).collect { case JsBoolean(x) => x }
                .getOrElse(false)
              JsSuccess(
                CheckBoxValue(state)
              )

            case _ =>
              JsError(s"Unexpected type: $tpe")
          }

        case _ =>
          JsError("Object is required")
      }
    }
  }

  implicit lazy val globalSettingsFormat: Format[Settings] = Json.format[Settings]

}
