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

    case class MarkAsReadSource(num: Long) extends BaseMessage
    case class MarkAsUnreadSource(num: Long) extends BaseMessage
  }


}
