package truerss.api

import akka.actor._
import akka.event.{EventStream, LoggingAdapter}
import java.net.InetSocketAddress

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import truerss.system.ws._
import truerss.system.util.SourceDeleted
import truerss.system.util.Notify

class WSApi(val port: Int) extends Actor with ActorLogging {

  log.info("WS Api start...")

  implicit val system = context.system
  val stream = system.eventStream

  val socketServer = new SocketServer(port, context, stream, log)
  socketServer.start()

  def receive = { case x => log.warning(s"Unexpected message ${x}") }

}

case class SocketServer(port: Int,
                        ctx: ActorContext,
                        stream: EventStream,
                        log: LoggingAdapter) extends
WebSocketServer(new InetSocketAddress(port)) {

  val connectionMap = scala.collection.mutable.Map[WebSocket, ActorRef]()

  override def onOpen(ws: WebSocket, clientHandshake: ClientHandshake): Unit = {
    log.info(s"ws connection open")
    val socketActor = ctx.actorOf(Props(classOf[WSController], ws))
    stream.subscribe(socketActor, classOf[NewFeeds])
    stream.subscribe(socketActor, classOf[SourceAdded])
    stream.subscribe(socketActor, classOf[SourceDeleted])
    stream.subscribe(socketActor, classOf[SourceUpdated])
    stream.subscribe(socketActor, classOf[Notify])
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
