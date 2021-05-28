package truerss.api.ws

import akka.actor.{Actor, ActorLogging}
import org.java_websocket.WebSocket
import play.api.libs.json._
import truerss.dto.{FeedDto, SourceViewDto}

// one actor per one user connection
class WebSocketController(private val ws: WebSocket) extends Actor with ActorLogging {

  import WebSocketController._
  import WebSocketJsonFormats._

  def receive: Receive = {
    case m: WSNotifyMessage =>
      ws.send(Json.toJson(m.toData).toString())
  }

  override def postStop(): Unit = {
    ws.close()
  }

}

object WebSocketController {

  sealed trait WSNotifyMessage
  case class NotifyNewFeeds(newFeeds: Iterable[FeedDto]) extends WSNotifyMessage
  case class NotifyMessage(message: Notify) extends WSNotifyMessage
  case class NotifySourceError(sourceId: Long, message: Notify) extends WSNotifyMessage
  case class NotifyNewSource(newSource: SourceViewDto) extends WSNotifyMessage

  implicit class WSMessageExt(val m: WSNotifyMessage) extends AnyVal {
    def toData: WebSocketData = {
      m match {
        case NotifyNewFeeds(newFeeds) =>
          WebSocketNewFeedsMessage(newFeeds)
        case NotifyMessage(message) =>
          WebSocketNotifyMessage(message.message, message.level)
        case NotifySourceError(sourceId, message) =>
          WebSocketNotifySourceErrorMessage(sourceId, message.message, message.level)
        case NotifyNewSource(newSource) =>
          WebSocketNewSourceMessage(newSource)
      }
    }
  }

}