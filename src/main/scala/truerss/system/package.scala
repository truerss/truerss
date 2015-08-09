package truerss

import truerss.models.Source

/**
 * Created by mike on 2.8.15.
 */
package object system {

  // for communication with db
  object db {
    sealed trait BaseMessage
    case object GetAll extends BaseMessage
    case class GetSource(num: Long) extends BaseMessage
    case class AddSource(source: Source) extends BaseMessage
    case class DeleteSource(num: Long) extends BaseMessage
    case class UpdateSource(num: Long, source: Source) extends BaseMessage

    case object Favorites extends BaseMessage
    case class GetFeed(num: Long) extends BaseMessage
    case class MarkFeed(num: Long) extends BaseMessage
    case class UnmarkFeed(num: Long) extends BaseMessage
    case class MarkAsReadFeed(num: Long) extends BaseMessage
    case class MarkAsUnreadFeed(num: Long) extends BaseMessage

    case class MarkAll(num: Long) extends BaseMessage


    // util:
    case class UrlIsUniq(url: String, id: Option[Long] = None) extends BaseMessage
    case class NameIsUniq(name: String, id: Option[Long] = None) extends BaseMessage

  }


}
