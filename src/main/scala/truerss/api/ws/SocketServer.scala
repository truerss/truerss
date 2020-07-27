package truerss.api.ws

import java.net.InetSocketAddress

import akka.actor.{ActorContext, ActorRef, Props}
import akka.event.EventStream
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.slf4j.LoggerFactory

case class SocketServer(port: Int,
                        ctx: ActorContext,
                        stream: EventStream)
  extends WebSocketServer(new InetSocketAddress(port)) {

  private val logger = LoggerFactory.getLogger(getClass)

  private val connectionMap = scala.collection.mutable.Map[WebSocket, ActorRef]()

  override def onStart(): Unit = {
    logger.info(s"socket server started, port: $port")
  }

  override def onOpen(ws: WebSocket, clientHandshake: ClientHandshake): Unit = {
    logger.info(s"ws connection open")
    val socketActor = ctx.actorOf(Props(classOf[WebSocketController], ws))
    stream.subscribe(socketActor, classOf[WebSocketController.WSMessage])
    connectionMap(ws) = socketActor
  }

  override def onClose(webSocket: WebSocket, code: Int, reason: String, remote: Boolean): Unit = {
    logger.info("connection close")
    stop(webSocket)
  }

  override def onMessage(webSocket: WebSocket, message: String): Unit = {
    logger.info(s"message given: $message")
  }

  override def onError(webSocket: WebSocket, exception: Exception): Unit = {
    logger.info("connection error")
    stop(webSocket)
  }

  private def stop(webSocket: WebSocket): Unit = {
    Option(webSocket).foreach { key =>
      val actor = connectionMap(key)
      connectionMap -= webSocket
      stream.unsubscribe(actor)
      ctx.stop(actor)
    }
  }

}