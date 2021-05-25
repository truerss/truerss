package truerss.api.ws

import play.api.libs.json._
import truerss.api.JsonFormats

object WebSocketJsonFormats {

  import JsonFormats.{feedDtoFormatWrites, sourceViewDtoWrites}

  def convert[T: Writes](x: T): String = Json.stringify(Json.toJson(x))

  implicit lazy val wSMessageTypeWrites = new Writes[WSMessageType.type] {
    override def writes(o: WSMessageType.type): JsValue = {
      JsString(o.toString())
    }
  }

  implicit lazy val wsMessageWrites: Writes[WebSocketNotifyMessage] = Json.writes
  implicit lazy val wsNewFeedsWrites: Writes[WebSocketNewFeedsMessage] = Json.writes
  implicit lazy val wsNotifyWErrorWrites: Writes[WebSocketNotifySourceErrorMessage] = Json.writes
  implicit lazy val wsNewSourceWrites: Writes[WebSocketNewSourceMessage] = Json.writes
  implicit lazy val wsDateWrites: Writes[WebSocketData] = new Writes[WebSocketData] {
    private final val f: Writes[WebSocketData] = Json.writes
    override def writes(o: WebSocketData): JsValue = {
      f.writes(o).as[JsObject] + ("type" -> JsString(o.messageType.toString))
    }
  }
}
