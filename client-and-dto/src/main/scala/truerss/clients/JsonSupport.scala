package truerss.clients

import truerss.dto._
import java.util.Date
import play.api.libs.json._

object JsonSupport {

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

  implicit lazy val unitReads: Reads[Unit] = new Reads[Unit] {
    override def reads(json: JsValue): JsResult[Unit] = {
      JsSuccess(())
    }
  }

  implicit lazy val unitWrites: Writes[Unit] = new Writes[Unit] {
    override def writes(o: Unit): JsValue = {
      JsNull
    }
  }

  implicit lazy val processingReads: Reads[Processing] = new Reads[Processing] {
    override def reads(json: JsValue): JsResult[Processing] = {
      JsSuccess(Processing())
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
  implicit lazy val feedDtoFormat = Json.format[FeedDto]

  implicit lazy val newSourceFromFile = Json.format[NewSourceFromFileWithErrors]

  implicit lazy val availableValueReads: Reads[AvailableValue] = new Reads[AvailableValue] {
    override def reads(json: JsValue): JsResult[AvailableValue] = {
      json match {
        case JsObject(map) if map.keys.exists(_ == "type") =>
          JsSuccess(AvailableRadio)
        case JsArray(xs) =>
          JsSuccess(AvailableSelect(xs.collect { case JsNumber(x) => x.toInt }))
        case _ =>
          JsError("Invalid format")
      }
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

  implicit lazy val availableSetupReads: Reads[AvailableSetup[_]] = new Reads[AvailableSetup[_]] {
    override def reads(json: JsValue): JsResult[AvailableSetup[_]] = {
      val key = (json \ "key").as[String]
      val description = (json \ "description").as[String]
      val value = (json \ "value").as[CurrentValue[_]]
      val options = (json \ "options").as[AvailableValue]
      JsSuccess(
        AvailableSetup(
          key = key,
          description = description,
          value = value,
          options = options
        )
      )
    }
  }

  implicit lazy val newSetupWriter: Writes[NewSetup[_]] = new Writes[NewSetup[_]] {
    private final val fKey = "key"
    private final val fValue = "value"

    override def writes(o: NewSetup[_]): JsValue = {
      JsObject(
        Seq(
          fKey -> o.key.j,
          fValue -> currentValueWrites.writes(o.value)
        )
      )
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

  implicit def pageReader[T](implicit f: Reads[T]): Reads[Page[T]] = new Reads[Page[T]] {
    override def reads(json: JsValue): JsResult[Page[T]] = {
      json match {
        case JsObject(obj) =>
          val tmp = for {
            total <- obj.get("total").collect { case JsNumber(x) => x.toInt }
            resources <- obj.get("resources").collect {
              case JsArray(xs) => xs.map(f.reads).collect {
                case JsSuccess(x, _) => x
              }
            }
          } yield Page(total, resources)

          tmp.map { x => JsSuccess(x) }.getOrElse(JsError("Failed to deserialize"))

        case _ =>
          JsError("Object is required")
      }
    }
  }

  implicit val pageFeedsReader = pageReader[FeedDto]

  implicit lazy val reasonableError: Reads[ReasonableError] = new Reads[ReasonableError] {
    override def reads(json: JsValue): JsResult[ReasonableError] = {
      json match {
        case JsObject(obj) if obj.contains("errors") =>
          JsSuccess(BadRequestError(obj("errors").as[Iterable[String]]))

        case JsArray(values) =>
          JsSuccess(BadRequestError(values.collect { case JsString(x) => x }))

        case JsString(str) =>
          JsSuccess(ContentReadError(str))

        case _ =>
          JsError("Unexpected error in parsing")
      }
    }
  }

}
