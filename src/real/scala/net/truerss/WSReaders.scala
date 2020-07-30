package net.truerss

import play.api.libs.json._
import truerss.api.JsonFormats
import truerss.api.ws.WebSocketController.{NewFeeds, NewSource, NotifyMessage, WSMessage}
import truerss.dto.{FeedDto, Notify, NotifyLevel, SourceViewDto, WSMessageType}

object WSReaders {

  import JsonFormats.{feedDtoFormat, sourceViewDtoFormat}

  private implicit lazy val notifyLevelsReads: Reads[NotifyLevel.Value] = Reads.enumNameReads(NotifyLevel)

  private implicit lazy val notifyReads: Reads[Notify] = Json.reads[Notify]

  implicit lazy val wsMessageReaders: Reads[WSMessage] = new Reads[WSMessage] {
    override def reads(jsValue: JsValue): JsResult[WSMessage] = {
      jsValue match {
        case JsString(value) =>
          val json = Json.parse(value)
          val tpe = WSMessageType.withName((json \ "messageType").as[String])
          tpe match {
            case WSMessageType.NewSource =>
              val x = (json \ "body").as[SourceViewDto]
              JsSuccess(NewSource(x))

            case WSMessageType.New =>
              val xs = (json \ "body").as[Iterable[FeedDto]]
              JsSuccess(NewFeeds(xs))

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
