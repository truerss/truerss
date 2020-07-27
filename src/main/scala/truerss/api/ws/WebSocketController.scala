package truerss.api.ws

import akka.actor.{Actor, ActorLogging}
import org.java_websocket.WebSocket
import truerss.api.JsonFormats
import truerss.dto.{FeedDto, Notify, WSMessageType, WebSocketMessage}

class WebSocketController(ws: WebSocket) extends Actor with ActorLogging {

  import WebSocketController._

  def receive: Receive = {
    case m: WSMessage =>
      ws.send(m.asJsonString)
  }

}

object WebSocketController {

  import JsonFormats.feedDtoFormat
  import WebSocketJsonFormats._

  sealed trait WSMessage
  case class NewFeeds(xs: Iterable[FeedDto]) extends WSMessage
  case class NotifyMessage(message: Notify) extends WSMessage

  implicit class WSMessageJson(val x: WSMessage) extends AnyVal {
    def asJsonString: String = {
      x match {
        case NewFeeds(xs) =>
          convert(WebSocketMessage(WSMessageType.New, convert(xs)))

        case NotifyMessage(message) =>
          s"${convert(WebSocketMessage(WSMessageType.Notify, convert(message)))}"
      }
    }
  }

}
