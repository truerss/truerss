package truerss.api.ws

import akka.actor.{Actor, ActorLogging}
import org.java_websocket.WebSocket
import play.api.libs.json._
import truerss.dto.{FeedDto, SourceViewDto}
import truerss.json.JsonFormats

// one actor per one user connection
class WebSocketController(private val ws: WebSocket) extends Actor with ActorLogging {

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

  import JsonFormats.{feedDtoFormat, sourceViewDtoFormat}
  import WebSocketJsonFormats._

  sealed trait WSMessage
  case class NewFeeds(newFeeds: Iterable[FeedDto]) extends WSMessage
  case class NotifyMessage(message: Notify) extends WSMessage
  case class NewSource(newSource: SourceViewDto) extends WSMessage

  object Fields {
    final val mtF = "messageType"
    final val bF = "body"
  }

  implicit class WSMessageJson(val x: WSMessage) extends AnyVal {
    import Fields._

    def asJsonString: String = {
      val result = x match {
        case NewSource(x) =>
          JsObject(
            Seq(
              mtF -> JsString(WSMessageType.NewSource.toString),
              bF -> Json.toJson(x)
            )
          )

        case NewFeeds(xs) =>
          JsObject(
            Seq(
              mtF -> JsString(WSMessageType.New.toString),
              bF -> Json.toJson(xs)
            )
          )

        case NotifyMessage(message) =>
          JsObject(
            Seq(
              mtF -> JsString(WSMessageType.Notify.toString),
              bF -> Json.toJson(message)
            )
          )
      }

      Json.stringify(result)
    }
  }

}