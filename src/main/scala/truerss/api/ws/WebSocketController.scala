package truerss.api.ws

import akka.actor.{Actor, ActorLogging}
import org.java_websocket.WebSocket
import truerss.api.JsonFormats
import play.api.libs.json._
import truerss.dto.{FeedDto, Notify, WSMessageType}

class WebSocketController(ws: WebSocket) extends Actor with ActorLogging {

  import WebSocketController._

  def receive: Receive = {
    case m: WSMessage =>
      ws.send(m.asJsonString)
  }

  override def postStop(): Unit = {
    ws.close()
  }

}

object WebSocketController {

  import JsonFormats.feedDtoFormat
  import WebSocketJsonFormats._

  sealed trait WSMessage
  case class NewFeeds(newFeeds: Iterable[FeedDto]) extends WSMessage
  case class NotifyMessage(message: Notify) extends WSMessage

  implicit class WSMessageJson(val x: WSMessage) extends AnyVal {
    def asJsonString: String = {
      val result = x match {
        case NewFeeds(xs) =>
          JsObject(
            Seq(
              "messageType" -> JsString(WSMessageType.New.toString),
              "body" -> Json.toJson(xs)
            )
          )

        case NotifyMessage(message) =>
          JsObject(
            Seq(
              "messageType" -> JsString(WSMessageType.Notify.toString),
              "body" -> Json.toJson(message)
            )
          )
      }

      Json.stringify(result)
    }
  }

}
