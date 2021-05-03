package truerss.api.ws

import play.api.libs.json._
import truerss.dto.Notify

object WebSocketJsonFormats {

  def convert[T: Writes](x: T): String = Json.stringify(Json.toJson(x))

  implicit val notifyLevelWrites: Writes[Notify] = Json.writes[Notify]

  implicit lazy val wSMessageTypeWrites = new Writes[WSMessageType.type ] {
    override def writes(o: WSMessageType.type): JsValue = {
      JsString(o.toString())
    }
  }

  implicit lazy val wsMessageWrites = Json.writes[WebSocketMessage]
}
