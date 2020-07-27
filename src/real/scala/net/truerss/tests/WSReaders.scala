package net.truerss.tests

import play.api.libs.json._
import truerss.api.ws.WebSocketController.{NewFeeds, NotifyMessage, WSMessage}
import truerss.dto.{FeedDto, Notify, NotifyLevel, WSMessageType}
import truerss.api.ws.WebSocketJsonFormats
import truerss.api.JsonFormats

object WSReaders {

  import JsonFormats.feedDtoFormat
  import WebSocketJsonFormats._

  private implicit lazy val notifyLevelsReads: Reads[NotifyLevel.Value] = Reads.enumNameReads(NotifyLevel)

  private implicit lazy val notifyReads: Reads[Notify] = Json.reads[Notify]

  implicit lazy val wsMessageReaders: Reads[WSMessage] = new Reads[WSMessage] {
    override def reads(jsValue: JsValue): JsResult[WSMessage] = {
      jsValue match {
        case JsString(value) =>
          val json = Json.parse(value)
          val tpe = WSMessageType.withName((json \ "messageType").as[String])
          tpe match {
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
