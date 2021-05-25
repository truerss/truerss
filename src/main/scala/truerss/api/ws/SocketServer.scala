package truerss.api.ws

import java.net.InetSocketAddress

import akka.actor.{ActorRef, ActorSystem, Props}
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import org.slf4j.LoggerFactory

case class SocketServer(port: Int, system: ActorSystem)
  extends WebSocketServer(new InetSocketAddress(port)) {

  private val logger = LoggerFactory.getLogger(getClass)
  private val stream = system.eventStream

  private val connectionMap = scala.collection.mutable.Map[WebSocket, ActorRef]()

  override def onStart(): Unit = {
    logger.info(s"socket server started, port: $port")
  }

  override def onOpen(ws: WebSocket, clientHandshake: ClientHandshake): Unit = {
    logger.info(s"ws connection open")
    val socketActor = system.actorOf(Props(classOf[WebSocketController], ws))
    stream.subscribe(socketActor, classOf[WebSocketController.WSNotifyMessage])
    connectionMap(ws) = socketActor
  }

  override def onClose(webSocket: WebSocket, code: Int, reason: String, remote: Boolean): Unit = {
    logger.info("connection close")
    stopConnection(webSocket)
  }

  override def onMessage(webSocket: WebSocket, message: String): Unit = {
    logger.info(s"message given: $message")
  }

  override def onError(webSocket: WebSocket, exception: Exception): Unit = {
    logger.info("connection error")
    stopConnection(webSocket)
  }


  override def stop(): Unit = {
    connectionMap.foreach { case (ws, _) =>
      stopConnection(ws)
    }
    super.stop()
  }

  private def stopConnection(webSocket: WebSocket): Unit = {
    Option(webSocket).foreach { key =>
      val actor = connectionMap(key)
      connectionMap -= webSocket
      stream.unsubscribe(actor)
      system.stop(actor)
    }
  }

}