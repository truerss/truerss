package truerss.api

import akka.actor.{Actor, ActorLogging}
import org.java_websocket.WebSocket
import truerss.dto.WSMessage
import truerss.dto.{FeedDto, Notify, SourceViewDto}

class WebSockerController(ws: WebSocket) extends Actor with ActorLogging {

  import JsonFormats._
  import WebSockerController._

  def receive = {
    case SourceAdded(source) =>
      ws.send(s"${J(WSMessage("create", J(source)))}")
    case NewFeeds(xs) =>
      ws.send(s"${J(WSMessage("new", J(xs)))}")
    case SourceDeleted(source) =>
      ws.send(s"${J(WSMessage("deleted", J(source)))}")
    case SourceUpdated(source) =>
      ws.send(s"${J(WSMessage("updated", J(source)))}")
    case msg: Notify =>
      ws.send(s"${J(WSMessage("notify", J(msg)))}")
    case _ => //ws.send()
  }

}

object WebSockerController {
  sealed trait WSMessage
  case class SourceAdded(source: SourceViewDto) extends WSMessage
  case class SourceUpdated(source: SourceViewDto) extends WSMessage
  case class NewFeeds(xs: Vector[FeedDto]) extends WSMessage
  case class SourceDeleted(source: SourceViewDto) extends WSMessage
}
