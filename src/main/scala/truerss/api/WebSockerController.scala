package truerss.api

import akka.actor.{Actor, ActorLogging}
import org.java_websocket.WebSocket
import truerss.dto.WSMessage
import truerss.dto.{FeedDto, Notify, SourceViewDto}

class WebSockerController(ws: WebSocket) extends Actor with ActorLogging {

  import JsonFormats._
  import WebSockerController._

  def receive = {
    case NewFeeds(xs) =>
      ws.send(s"${J(WSMessage("new", J(xs)))}")
    case msg: Notify =>
      ws.send(s"${J(WSMessage("notify", J(msg)))}")
    case _ => //ws.send()
  }

}

object WebSockerController {
  sealed trait WSMessage
  case class NewFeeds(xs: Vector[FeedDto]) extends WSMessage
}
