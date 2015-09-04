package truerss.controllers

import akka.actor.{ActorLogging, Actor}
import org.java_websocket.WebSocket

import truerss.system.ws.{SourceUpdated, SourceAdded, NewFeeds}
import truerss.system.util.SourceDeleted
import truerss.models.{ApiJsonProtocol, WSMessage}
import spray.json._

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
    case _ => //ws.send()
  }

}
