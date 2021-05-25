package net.truerss

import play.api.libs.json._
import truerss.clients.JsonSupport
import truerss.api.ws.{Notify, NotifyLevel, WSMessageType}
import truerss.api.ws.WebSocketController.{NotifyNewFeeds, NotifyNewSource, NotifyMessage, NotifySourceError, WSNotifyMessage}
import truerss.dto.{FeedDto, SourceViewDto}

object WSReaders {

  import JsonSupport.{feedDtoReads, sourceViewDtoReads}

  private implicit lazy val notifyLevelsReads: Reads[NotifyLevel.Value] = Reads.enumNameReads(NotifyLevel)

  private implicit lazy val notifyReads: Reads[Notify] = Json.reads[Notify]

  implicit lazy val wsMessageReaders: Reads[WSNotifyMessage] = new Reads[WSNotifyMessage] {
    override def reads(jsValue: JsValue): JsResult[WSNotifyMessage] = {
      jsValue match {
        case JsString(value) =>
          val json = Json.parse(value)
          val tpe = WSMessageType.withName((json \ "messageType").as[String])
          tpe match {
            case WSMessageType.NewSource =>
              val x = (json \ "body").as[SourceViewDto]
              JsSuccess(NotifyNewSource(x))

            case WSMessageType.New =>
              val xs = (json \ "body").as[Iterable[FeedDto]]
              JsSuccess(NotifyNewFeeds(xs))

            case WSMessageType.SourceError =>
              val notify = (json \ "body").as[Notify]
              val sourceId = (json \ "sourceId").as[Long]
              JsSuccess(NotifySourceError(sourceId, notify))

            case WSMessageType.Notify =>
              val notify = (json \ "body").as[Notify]
              JsSuccess(NotifyMessage(notify))
          }

        case _ =>
          throw new IllegalStateException("String is required")
      }

    }
  }

}
