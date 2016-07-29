package truerss

import akka.actor.ActorRef
import truerss.models.{SourceState, Source, Feed}
import com.github.truerss.base.{Entry, BaseFeedReader, BaseContentReader}

package object system {

  sealed trait ApiMessage
  // for communication with db
  object db {
    trait Numerable { val num: Long }
    trait Sourcing { val source: Source }

    case object OnlySources extends ApiMessage
    case object GetAll extends ApiMessage
    case object MarkAll extends ApiMessage // mark all sources as read
    case class GetSource(num: Long) extends ApiMessage with Numerable
    case class Mark(num: Long) extends ApiMessage with Numerable
    case class DeleteSource(num: Long) extends ApiMessage with Numerable
    case class AddSource(source: Source) extends ApiMessage with Sourcing
    case class UpdateSource(num: Long, source: Source) extends ApiMessage with Sourcing

    case object Favorites extends ApiMessage
    case class GetFeed(num: Long) extends ApiMessage
    case class MarkFeed(num: Long) extends ApiMessage
    case class UnmarkFeed(num: Long) extends ApiMessage
    case class MarkAsReadFeed(num: Long) extends ApiMessage
    case class MarkAsUnreadFeed(num: Long) extends ApiMessage

    case class AddFeeds(sourceId: Long, xs: Vector[Entry]) extends ApiMessage


    case class Latest(count: Long) extends ApiMessage
    case class ExtractFeedsForSource(sourceId: Long, from: Int = 0, limit: Int = Int.MaxValue) extends ApiMessage

    // util:
    case class UrlIsUniq(url: String, id: Option[Long] = None) extends ApiMessage
    case class NameIsUniq(name: String, id: Option[Long] = None) extends ApiMessage
    case class FeedCount(read: Boolean = false) extends ApiMessage
    case class FeedCountForSource(sourceId: Long) extends ApiMessage

    case class SetState(sourceId: Long, state: SourceState) extends ApiMessage

    // responses
    case class ResponseSources(xs: Vector[Source])
    case class ResponseMaybeSource(source: Option[Source])
    case class ResponseFeeds(xs: Vector[Feed])
    case class ResponseMaybeFeed(feed: Option[Feed])
    case class ResponseFeedCount(response: Vector[(Long, Int)])
    case class ResponseCount(count: Int)
    case class ResponseDone(id: Long)
    case class ResponseFeedCheck(count: Int)
    case class ResponseSourceId(id: Long)


  }

  object network {
    case class ExtractContent(sourceId: Long, feedId: Long, url: String)

    sealed trait NetworkResult

    case class ExtractedEntries(sourceId: Long, entries: Vector[Entry]) extends NetworkResult
    case class ExtractContentForEntry(sourceId: Long, feedId: Long, content: Option[String]) extends NetworkResult
    case class ExtractError(message: String) extends NetworkResult
    case class SourceNotFound(sourceId: Long) extends NetworkResult
  }

  object plugins {
    case object GetPluginList extends ApiMessage
  }

  object global {
    case object RestartSystem extends ApiMessage
    case object StopSystem extends ApiMessage
    case object StopApp extends ApiMessage
  }

  object util {
    case object Start
    case object Update extends ApiMessage
    case object Updated
    case class UpdateMe(who: ActorRef)
    case class UpdateOne(num: Long) extends ApiMessage
    case class Unread(sourceId: Long) extends ApiMessage

    case object Opml extends ApiMessage

    case class SourceDeleted(source: Source)
    case class SourceLastUpdate(sourceId: Long)
    case class FeedContentUpdate(feedId: Long, content: String)

    case class NewSource(source: Source)
    case class ReloadSource(source: Source)
    case class StopSource(source: Source) //TODO use

    case class PublishEvent(feed: Feed)

    object NotifyLevels {
      sealed trait Level { val name: String }
      case object Info extends Level { val name = "info" }
      case object Warning extends Level { val name = "warning" }
      case object Danger extends Level { val name = "danger" }
    }

    case class Notify(level: NotifyLevels.Level, message: String)
  }

  object ws {
    case class SourceAdded(source: Source)
    case class SourceUpdated(source: Source)
    case class NewFeeds(xs: Vector[Feed])
  }

}
