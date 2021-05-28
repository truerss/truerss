package truerss.api.ws

import truerss.dto.{FeedDto, SourceViewDto}

// ws protocol
object WSMessageType extends Enumeration {
  // todo rename
  val New, Notify, SourceError, NewSource = Value
}

sealed trait WebSocketData {
  val messageType: WSMessageType.Value
}
case class WebSocketNotifyMessage(message: String, level: NotifyLevel.Value) extends WebSocketData {
  override val messageType: WSMessageType.Value = WSMessageType.Notify
}
case class WebSocketNewFeedsMessage(newFeeds: Iterable[FeedDto]) extends WebSocketData {
  override val messageType: WSMessageType.Value = WSMessageType.New
}
case class WebSocketNotifySourceErrorMessage(sourceId: Long,
                                             message: String,
                                             level: NotifyLevel.Value) extends WebSocketData {
  override val messageType: WSMessageType.Value = WSMessageType.SourceError
}
case class WebSocketNewSourceMessage(newSource: SourceViewDto) extends WebSocketData {
  override val messageType: WSMessageType.Value = WSMessageType.NewSource
}
