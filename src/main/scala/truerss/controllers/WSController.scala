package truerss.controllers

import akka.actor.{ActorLogging, Actor}
import org.java_websocket.WebSocket
import org.java_websocket.framing.Framedata

/**
 * Created by mike on 29.8.15.
 */
class WSController(ws: WebSocket) extends Actor with ActorLogging {

  def receive = {
    case _ => //ws.send()
  }

  override def postStop: Unit = {
    log.info(s"${ws} stopped")
  }

}
