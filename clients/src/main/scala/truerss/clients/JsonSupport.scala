package truerss.clients

import truerss.dto._
import play.api.libs.json._

object JsonSupport {

  implicit class StringExtJson(val x: String) extends AnyVal {
    def j: JsString = JsString(x)
  }

  implicit lazy val unitReads: Reads[Unit] = new Reads[Unit] {
    override def reads(json: JsValue): JsResult[Unit] = {
      JsSuccess(())
    }
  }

  implicit lazy val processingReads: Reads[Processing] = new Reads[Processing] {
    override def reads(json: JsValue): JsResult[Processing] = {
      JsSuccess(Processing())
    }
  }

  implicit val stateReads = Reads.enumNameReads(State)

  implicit lazy val newSourceDtoWrites = Json.writes[NewSourceDto]
  implicit lazy val updateSourceDtoWrites = Json.writes[UpdateSourceDto]
  implicit lazy val sourceViewDtoReads = Json.reads[SourceViewDto]

  implicit lazy val pluginDtoReads = Json.reads[PluginDto]
  implicit lazy val pluginsViewDtoReads = Json.reads[PluginsViewDto]
  implicit lazy val feedDtoReads = Json.reads[FeedDto]

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

  implicit val feedsFrequencyReads: Reads[FeedsFrequency] = Json.reads
  implicit val sourceOverviewReads: Reads[SourceOverview] = Json.reads

  implicit val feedContentReads: Reads[FeedContent] = Json.reads

  implicit val searchRequestWrites: Writes[SearchRequest] = Json.writes

  implicit val newPluginSourceWrites: Writes[NewPluginSource] = Json.writes
  implicit val pluginSourceDtoReads: Reads[PluginSourceDto] = Json.reads
  implicit val installPluginWrites: Writes[InstallPlugin] = Json.writes
  implicit val uninstallPluginWrites: Writes[UninstallPlugin] = Json.writes

  implicit val sourceStatusDtoReads: Reads[SourceStatusDto] = Json.reads

  implicit def pageReader[T](implicit f: Reads[T]): Reads[Page[T]] = Json.reads

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
