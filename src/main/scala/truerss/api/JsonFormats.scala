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
    private final val r = JsObject(Seq("type" -> "radio".j))
    override def writes(o: AvailableValue): JsValue = {
      o match {
        case AvailableSelect(predefined) =>
          JsArray(predefined.map(x => JsNumber(x)).toSeq)

        case AvailableRadio(_) =>
          r
      }
    }

    override def reads(json: JsValue): JsResult[AvailableValue] = {
      json match {
        case JsBoolean(b) =>
          JsSuccess(AvailableRadio(b))
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
        case JsNumber(o) => JsSuccess(CurrentValue[Int](o.toInt))
        case JsBoolean(o) => JsSuccess(CurrentValue[Boolean](o))
        case JsString(o) => JsSuccess(CurrentValue[String](o))
        case x => JsError(s"Unkwnown type: $x")
      }
    }
  }

  implicit lazy val availableSetupWrites: Writes[AvailableSetup[_]] = new Writes[AvailableSetup[_]] {
    override def writes(o: AvailableSetup[_]): JsValue = {
      JsObject(
        Seq(
          "key" -> o.key.j,
          "description" -> o.description.j,
          "options" -> availableValueFormat.writes(o.options),
          "value" -> currentValueFormat.writes(o.value)
        )
      )
    }
  }

  implicit lazy val newSetupFormat: Format[NewSetup[_]] = new Format[NewSetup[_]] {
    private final val fKey = "key"
    private final val fValue = "value"
    override def reads(json: JsValue): JsResult[NewSetup[_]] = {
      json match {
        case JsObject(obj) =>
          val r = for {
            k <- obj.get(fKey).collect { case JsString(x) => x }
            v <- obj.get(fValue).map(currentValueFormat.reads)
            cv <- v.asOpt
          } yield {
            cv match {
              case CurrentValue(x: Int) =>
                JsSuccess(NewSetup[Int](k, CurrentValue(x)))
              case CurrentValue(x: String) =>
                JsSuccess(NewSetup[String](k, CurrentValue(x)))
              case CurrentValue(x: Boolean) =>
                JsSuccess(NewSetup[Boolean](k, CurrentValue(x)))
              case _ =>
                JsError(s"Unexpected type: $cv")
            }
          }
          r.getOrElse(JsError(s"Unexpected object: $obj"))
        case _ =>
          JsError("Object is required")
      }
    }

    override def writes(o: NewSetup[_]): JsValue = {
      JsObject(
        Seq(
          "key" -> o.key.j,
          "value" -> currentValueFormat.writes(o.value)
        )
      )
    }
  }

  implicit val feedsFrequencyFormat: Format[FeedsFrequency] = Json.format[FeedsFrequency]
  implicit val sourceOverviewFormat: Format[SourceOverview] = Json.format[SourceOverview]

  implicit val feedContentFormat: Format[FeedContent] = Json.format[FeedContent]

  implicit val searchRequestFormat: Format[SearchRequest] = Json.format[SearchRequest]

}
