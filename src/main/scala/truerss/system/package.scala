package truerss

import truerss.models.{Feed, Source}
import truerss.plugins.Entry
/**
 * Created by mike on 2.8.15.
 */
package object system {

  sealed trait BaseMessage
  // for communication with db
  object db {


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

    case class AddFeeds(sourceId: Long, xs: Vector[Feed]) extends BaseMessage

    case class MarkAll(num: Long) extends BaseMessage
    case class Latest(count: Long) extends BaseMessage
    case class ExtractFeedsForSource(sourceId: Long) extends BaseMessage

    // util:
    case class UrlIsUniq(url: String, id: Option[Long] = None) extends BaseMessage
    case class NameIsUniq(name: String, id: Option[Long] = None) extends BaseMessage


  }

  object network {
    import truerss.plugins.BasePlugin
    case class SourceInfo(sourceId: Long, plugin: BasePlugin)
    case class Grep(sourceId: Long, url: String)
    case class ExtractContent(sourceId: Long, feedId: Long, url: String)
    case class NetworkInitialize(xs: Vector[SourceInfo])

    sealed trait NetworkResult

    case class ExtractedEntries(sourceId: Long, entries: Vector[Entry]) extends NetworkResult
    case class ExtractContentForEntry(sourceId: Long, feedId: Long, content: Option[String]) extends NetworkResult
    case class ExtractError(message: String) extends NetworkResult
    case class SourceNotFound(sourceId: Long) extends NetworkResult
  }

  object util {
    case object Start extends BaseMessage
    case object Update extends BaseMessage
    case class UpdateOne(num: Long) extends BaseMessage

    case class SourceLastUpdate(sourceId: Long)
    case class FeedContentUpdate(feedId: Long, content: String)
  }


}
