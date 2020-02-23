package truerss.api

import java.util.Date

import play.api.libs.json._
import truerss.db._
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

  implicit lazy val availableValueFormat: Format[AvailableValue] = new Format[AvailableValue] {
    override def writes(o: AvailableValue): JsValue = {
      o match {
        case AvailableSelect(predefined) =>
          JsArray(predefined.map(x => JsNumber(x)).toSeq)

        case AvailableCheckBox(currentState) =>
          JsBoolean(currentState)
      }
    }

    override def reads(json: JsValue): JsResult[AvailableValue] = {
      json match {
        case JsBoolean(b) =>
          JsSuccess(AvailableCheckBox(b))
        case JsArray(xs) =>
          JsSuccess(AvailableSelect(xs.collect { case JsNumber(x) => x.toInt }))
        case _ =>
          JsError("Invalid format")
      }
    }
  }

  implicit lazy val currentValueFormat: Format[CurrentValue[_]] = new Format[CurrentValue[_]] {
    override def writes(o: CurrentValue[_]): JsValue = {
      o match {
        case CurrentValue(o: Int) => JsNumber(o)
        case CurrentValue(o: Boolean) => JsBoolean(o)
        case CurrentValue(o: String) => JsString(o)
        case _ => JsNull
      }
    }

    override def reads(json: JsValue): JsResult[CurrentValue[_]] = {
      json match {
        case JsNumber(o) => JsSuccess(CurrentValue(o))
        case JsBoolean(o) => JsSuccess(CurrentValue(o))
        case JsString(o) => JsSuccess(CurrentValue(o))
        case x => JsError(s"Unkwnown type: $x")
      }
    }
  }

  implicit lazy val availableSetupFormat: Format[AvailableSetup[_]] = new Format[AvailableSetup[_]] {
    override def writes(o: AvailableSetup[_]): JsValue = {
      JsObject(
        Seq(
          "key" -> JsString(o.key),
          "description" -> JsString(o.description),
          "options" -> availableValueFormat.writes(o.options),
          "value" -> currentValueFormat.writes(o.current)
        )
      )
    }

    override def reads(json: JsValue): JsResult[AvailableSetup[_]] = ???
  }



}
