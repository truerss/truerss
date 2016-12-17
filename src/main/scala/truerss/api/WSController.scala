package truerss.api

import akka.actor.{Actor, ActorLogging}
import org.java_websocket.WebSocket
import spray.json._
import truerss.models.{ApiJsonProtocol, WSMessage}
import truerss.system.util.{Notify, SourceDeleted}
import truerss.system.ws.{NewFeeds, SourceAdded, SourceUpdated}

class WSController(ws: WebSocket) extends Actor with ActorLogging {

  import ApiJsonProtocol._

  def receive = {
    case SourceAdded(source) =>
      ws.send(s"${WSMessage("create", source.toJson.toString).toJson}")
    case NewFeeds(xs) =>
      ws.send(s"${WSMessage("new", xs.toJson.toString()).toJson}")
    case SourceDeleted(source) =>
      ws.send(s"${WSMessage("deleted", source.toJson.toString()).toJson}")
    case SourceUpdated(source) =>
      ws.send(s"${WSMessage("updated", source.toJson.toString()).toJson}")
    case msg: Notify =>
      ws.send(s"${WSMessage("notify", msg.toJson.toString).toJson}")
    case _ => //ws.send()
  }

}
