package net.truerss

import java.net.URI
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import play.api.libs.json.Json
import truerss.api.ws.Notify
import truerss.api.ws.WebSocketController.{NotifyNewFeeds, NotifyNewSource, NotifyMessage, NotifySourceError}
import truerss.dto.{FeedDto, SourceViewDto}

import scala.collection.mutable.{ArrayBuffer => AB}

class WSClient(val url: String) extends WebSocketClient(new URI(url)) {

  import WSReaders._

  val notifications = AB[Notify]()
  val newFeeds = AB[Iterable[FeedDto]]()
  val newSources = AB[SourceViewDto]()
  val sourceUpdateErrors = AB[NotifySourceError]()

  override def onOpen(handshakedata: ServerHandshake): Unit = {
  }

  override def onMessage(message: String): Unit = {
    val result = wsMessageReaders.reads(Json.toJson(message)).asOpt
    result match {
      case Some(value) =>
        value match {
          case NotifyNewSource(xs) =>
            newSources += xs

          case NotifyNewFeeds(feeds) =>
            newFeeds += feeds

          case msg: NotifySourceError =>
            sourceUpdateErrors += msg

          case NotifyMessage(notify) =>
            notifications += notify
        }
      case _ => onError(new RuntimeException(s"Unexpected result: $result"))
    }
  }

  override def onClose(code: Int, reason: String, remote: Boolean): Unit = {

  }

  override def onError(ex: Exception): Unit = {
    println(s"===============> $ex")
  }
}
