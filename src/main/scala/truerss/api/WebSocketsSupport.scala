package truerss.api

import akka.actor._
import akka.event.{EventStream, LoggingAdapter}
import java.net.InetSocketAddress

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer

class WebSocketsSupport(val port: Int) extends Actor with ActorLogging {

  log.info("WS Api start...")

  implicit val system = context.system
  val stream = system.eventStream

  val socketServer = SocketServer(port, context, stream, log)
  socketServer.start()

  def receive = { case x =>
    log.warning(s"Unexpected message $x")
  }

  override def postStop(): Unit = {
    socketServer.stop()
  }

}

case class SocketServer(port: Int,
                        ctx: ActorContext,
                        stream: EventStream,
                        log: LoggingAdapter) extends WebSocketServer(new InetSocketAddress(port)) {

  val connectionMap = scala.collection.mutable.Map[WebSocket, ActorRef]()

  override def onStart(): Unit = {
    log.info(s"socket server started, port: $port")
  }

  override def onOpen(ws: WebSocket, clientHandshake: ClientHandshake): Unit = {
    log.info(s"ws connection open")
    val socketActor = ctx.actorOf(Props(classOf[WebSockerController], ws))
    stream.subscribe(socketActor, classOf[WebSockerController.WSMessage])
    connectionMap(ws) = socketActor
  }

  override def onClose(webSocket: WebSocket, code: Int, reason: String, remote: Boolean): Unit = {
    log.info("connection close")
    stop(webSocket)
  }

  override def onMessage(webSocket: WebSocket, message: String): Unit = {
    log.info(s"message given: $message")
  }

  override def onError(webSocket: WebSocket, exception: Exception): Unit = {
    log.info("connection error")
    stop(webSocket)
  }

  private def stop(webSocket: WebSocket): Unit = {
    val actor = connectionMap(webSocket)
    connectionMap -= webSocket
    stream.unsubscribe(actor)
    ctx.stop(actor)
  }

}

object WebSocketsSupport {
  def props(port: Int): Props = {
    Props(classOf[WebSocketsSupport], port)
  }
}
