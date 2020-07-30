package truerss.dto

// ws protocol
object WSMessageType extends Enumeration {
  val New, Notify, NewSource = Value
}

case class WebSocketMessage(messageType: WSMessageType.Value, body: String)
