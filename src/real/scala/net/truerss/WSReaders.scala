package net.truerss

import play.api.libs.json._
import truerss.clients.JsonSupport
import truerss.api.ws.{NotifyLevel, WSMessageType, WebSocketData, WebSocketNewFeedsMessage, WebSocketNewSourceMessage, WebSocketNotifyMessage, WebSocketNotifySourceErrorMessage}

object WSReaders {

  import JsonSupport.{feedDtoReads, sourceViewDtoReads}

  private implicit lazy val notifyLevelsReads: Reads[NotifyLevel.Value] = Reads.enumNameReads(NotifyLevel)

  implicit lazy val wsMessageReaders: Reads[WebSocketData] = new Reads[WebSocketData] {
    private val wsMessageReads: Reads[WebSocketNotifyMessage] = Json.reads
    private val wsNewFeedsReads: Reads[WebSocketNewFeedsMessage] = Json.reads
    private val wsNotifyWErrorReads: Reads[WebSocketNotifySourceErrorMessage] = Json.reads
    private val wsNewSourceReads: Reads[WebSocketNewSourceMessage] = Json.reads
    override def reads(jsValue: JsValue): JsResult[WebSocketData] = {
      jsValue match {
        case JsString(value) =>
          val json = Json.parse(value)
          val tpe = WSMessageType.withName((json \ "type").as[String])
          tpe match {
            case WSMessageType.NewSource =>
              wsNewSourceReads.reads(json)

            case WSMessageType.New =>
              wsNewFeedsReads.reads(json)

            case WSMessageType.SourceError =>
              wsNotifyWErrorReads.reads(json)

            case WSMessageType.Notify =>
              wsMessageReads.reads(json)
          }

        case _ =>
          throw new IllegalStateException("String is required")
      }

    }
  }

}
