package truerss.api

import com.github.truerss.base.EnclosureType.jsonFormat
import play.api.libs.json._
import truerss.dto._

import java.util.Date

object JsonFormats {

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

  implicit lazy val processingWrites: Writes[Processing] = new Writes[Processing] {
    override def writes(o: Processing): JsValue = {
      JsNull
    }
  }

  implicit object StateFormat extends Format[State.Value] {
    override def reads(json: JsValue): JsResult[State.Value] = {
      json match {
        case JsNumber(value) =>
          State.values.find(_.id == value)
            .map(JsSuccess(_))
            .getOrElse(JsError(s"Invalid state: $value"))

        case _ =>
          JsError("Number required")
      }
    }

    override def writes(o: State.Value): JsValue = {
      JsNumber(o.id)
    }
  }

  implicit lazy val newSourceDtoFormat = Json.format[NewSourceDto]
  implicit lazy val updateSourceDtoFormat = Json.format[UpdateSourceDto]
  implicit lazy val sourceDtoFormat = Json.format[SourceDto]
  implicit lazy val sourceViewDtoFormat = Json.format[SourceViewDto]

  implicit lazy val pluginDtoFormat = Json.format[PluginDto]
  implicit lazy val pluginsViewDto = Json.format[PluginsViewDto]
  implicit lazy val enclosureDtoFormat = Json.format[EnclosureDto]
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

  implicit lazy val availableValueWrites: Writes[AvailableValue] = new Writes[AvailableValue] {
    private final val r = JsObject(Seq("type" -> "radio".j))
    override def writes(o: AvailableValue): JsValue = {
      o match {
        case AvailableSelect(predefined) =>
          JsArray(predefined.map(x => JsNumber(x)).toSeq)

        case AvailableRadio =>
          r // + ("value" -> JsBoolean(x))
      }
    }
  }

  implicit lazy val currentValueWrites: Writes[CurrentValue[_]] = new Writes[CurrentValue[_]] {
    override def writes(o: CurrentValue[_]): JsValue = {
      o match {
        case CurrentValue(o: Int) => JsNumber(o)
        case CurrentValue(o: Boolean) => JsBoolean(o)
        case CurrentValue(o: String) => JsString(o)
        case _ => JsNull
      }
    }
  }

  implicit lazy val availableSetupWrites: Writes[AvailableSetup[_]] = new Writes[AvailableSetup[_]] {
    override def writes(o: AvailableSetup[_]): JsValue = {
      JsObject(
        Seq(
          "key" -> o.key.j,
          "description" -> o.description.j,
          "options" -> availableValueWrites.writes(o.options),
          "value" -> currentValueWrites.writes(o.value)
        )
      )
    }
  }

  implicit lazy val currentValueReads: Reads[CurrentValue[_]] = new Reads[CurrentValue[_]] {
    override def reads(json: JsValue): JsResult[CurrentValue[_]] = {
      json match {
        case JsNumber(o) => JsSuccess(CurrentValue[Int](o.toInt))
        case JsBoolean(o) => JsSuccess(CurrentValue[Boolean](o))
        case JsString(o) => JsSuccess(CurrentValue[String](o))
        case x => JsError(s"Unknown type: $x")
      }
    }
  }

  implicit lazy val newSetupFormat: Reads[NewSetup[_]] = new Reads[NewSetup[_]] {
    private final val fKey = "key"
    private final val fValue = "value"
    override def reads(json: JsValue): JsResult[NewSetup[_]] = {
      json match {
        case JsObject(obj) =>
          val r = for {
            k <- obj.get(fKey).collect { case JsString(x) => x }
            v <- obj.get(fValue).map(currentValueReads.reads)
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
  }

  implicit val feedsFrequencyFormat: Format[FeedsFrequency] = Json.format[FeedsFrequency]
  implicit val sourceOverviewFormat: Format[SourceOverview] = Json.format[SourceOverview]

  implicit val feedContentFormat: Format[FeedContent] = Json.format[FeedContent]

  implicit val searchRequestFormat: Format[SearchRequest] = Json.format[SearchRequest]

  implicit val newPluginSourceFormat: Format[NewPluginSource] = Json.format[NewPluginSource]
  implicit val pluginSourceDtoFormat: Format[PluginSourceDto] = Json.format[PluginSourceDto]
  implicit val installPluginFormat: Format[InstallPlugin] = Json.format[InstallPlugin]
  implicit val uninstallPluginFormat: Format[UninstallPlugin] = Json.format[UninstallPlugin]

  implicit val sourceStatusDtoFormat: Format[SourceStatusDto] = Json.format[SourceStatusDto]

  implicit def pageWriter[T](implicit f: Writes[T]): Writes[Page[T]] = new Writes[Page[T]] {
    override def writes(o: Page[T]): JsValue = {
      JsObject(
        Seq(
          "total" -> JsNumber(o.total),
          "resources" -> JsArray(o.resources.map(x => f.writes(x)).toSeq)
        )
      )
    }
  }

  implicit val pageFeedsWriter = pageWriter[FeedDto]

}
