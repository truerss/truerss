package truerss.api.ws

import akka.actor._

// main actor in ws-hierarchy
class WebSocketsSupport(val port: Int) extends Actor with ActorLogging {

  log.info("WS Api start...")

  implicit val system = context.system
  val stream = system.eventStream

  val socketServer = SocketServer(port, context, stream)
  socketServer.start()

  def receive = {
    case x =>
      unhandled(x)
  }

  override def postStop(): Unit = {
    socketServer.stop()
  }

}


object WebSocketsSupport {
  def props(port: Int): Props = {
    Props(classOf[WebSocketsSupport], port)
  }
}
