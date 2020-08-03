package truerss.clients

import play.api.libs.json._

object JsonFormatsError {

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
