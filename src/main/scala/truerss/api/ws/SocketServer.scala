package truerss.api.ws

import java.net.InetSocketAddress
import io.truerss.actorika._
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.util.concurrent.{ConcurrentHashMap => CHM}
import scala.jdk.CollectionConverters._
import org.slf4j.LoggerFactory

case class SocketServer(port: Int, system: ActorSystem)
  extends WebSocketServer(new InetSocketAddress(port)) {

  import SocketServer._

  private val logger = LoggerFactory.getLogger(getClass)

  private val connectionMap = new CHM[WebSocket, ActorRef]()

  override def onStart(): Unit = {
    logger.info(s"socket server started, port: $port")
  }

  override def onOpen(ws: WebSocket, clientHandshake: ClientHandshake): Unit = {
    logger.info(s"ws connection open")
    val socketActor = system.spawn(new WebSocketController(ws), generator)
    system.subscribe(socketActor, classOf[WebSocketController.WSMessage])
    connectionMap.put(ws, socketActor)
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
    connectionMap.asScala.foreach { case (ws, _) =>
      stopConnection(ws)
    }
    super.stop()
  }

  private def stopConnection(webSocket: WebSocket): Unit = {
    Option(webSocket).foreach { key =>
      Option(connectionMap.remove(key)).foreach { ref =>
        system.unsubscribeAll(ref)
        system.stop(ref)
      }
    }
  }

}
object SocketServer {
  val generator = ActorNameGenerator.create("ws")
}