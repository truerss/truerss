package truerss.api

import play.api.libs.json._
import truerss.dto._

object JsonFormats {

  implicit class StringExtJson(val x: String) extends AnyVal {
    def j: JsString = JsString(x)
  }

  implicit lazy val processingWrites: Writes[Processing] = new Writes[Processing] {
    override def writes(o: Processing): JsValue = {
      JsNull
    }
  }

  implicit val stateWrites: Writes[State.Value] = Writes.enumNameWrites

  implicit lazy val newSourceDtoReads = Json.reads[NewSourceDto]
  implicit lazy val updateSourceDtoReads = Json.reads[UpdateSourceDto]
  implicit lazy val sourceViewDtoWrites = Json.writes[SourceViewDto]

  implicit lazy val pluginDtoWrites = Json.writes[PluginDto]
  implicit lazy val pluginsViewDtoWrites = Json.writes[PluginsViewDto]
  implicit lazy val feedDtoFormatWrites = Json.writes[FeedDto]

  implicit lazy val newSourceFromFile = Json.format[NewSourceFromFileWithErrors]

  implicit lazy val sourceImportResult: Writes[Map[Int, Either[NewSourceFromFileWithErrors, SourceViewDto]]] = new Writes[Map[Int, Either[NewSourceFromFileWithErrors, SourceViewDto]]] {
    override def writes(o: Map[Int, Either[NewSourceFromFileWithErrors, SourceViewDto]]): JsValue = {
      val r = o.map { case (k, v) =>
        val tmp = v match {
          case Left(x) => newSourceFromFile.writes(x)
          case Right(x) => sourceViewDtoWrites.writes(x)
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

  implicit val feedsFrequencyWrites = Json.writes[FeedsFrequency]
  implicit val sourceOverviewWrites = Json.writes[SourceOverview]

  implicit val feedContentWrites = Json.writes[FeedContent]

  implicit val searchRequestFormat = Json.format[SearchRequest]

  implicit val newPluginSourceReads = Json.reads[NewPluginSource]
  implicit val pluginSourceDtoWrites = Json.writes[PluginSourceDto]
  implicit val installPluginReads = Json.reads[InstallPlugin]
  implicit val uninstallPluginReads = Json.reads[UninstallPlugin]

  implicit val sourceStatusDtoWrites = Json.writes[SourceStatusDto]

  implicit def pageWriter[T](implicit f: Writes[T]): Writes[Page[T]] = Json.writes

  implicit val pageFeedsWriter = pageWriter[FeedDto]

}
