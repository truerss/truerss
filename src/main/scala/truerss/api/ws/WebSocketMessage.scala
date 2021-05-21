package truerss.api.ws

// ws protocol
object WSMessageType extends Enumeration {
  val New, Notify, SourceError, NewSource = Value
}

case class WebSocketMessage(messageType: WSMessageType.Value, body: String)
