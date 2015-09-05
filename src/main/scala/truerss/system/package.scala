package truerss

import akka.actor.ActorRef
import truerss.models.{SourceState, SourceForFrontend, Feed, Source}
import com.github.truerss.base.{Entry, BaseFeedReader, BaseContentReader}

package object system {

  sealed trait BaseMessage
  // for communication with db
  object db {
    // TODO remove this traits, make generic
    trait Numerable { val num: Long }
    trait Sourcing { val source: Source }

    case object OnlySources extends BaseMessage
    case object GetAll extends BaseMessage
    case class GetSource(num: Long) extends BaseMessage with Numerable
    case class MarkAll(num: Long) extends BaseMessage with Numerable
    case class DeleteSource(num: Long) extends BaseMessage with Numerable
    case class AddSource(source: Source) extends BaseMessage with Sourcing
    case class UpdateSource(num: Long, source: Source) extends BaseMessage with Sourcing

    case object Favorites extends BaseMessage
    case class GetFeed(num: Long) extends BaseMessage
    case class MarkFeed(num: Long) extends BaseMessage
    case class UnmarkFeed(num: Long) extends BaseMessage
    case class MarkAsReadFeed(num: Long) extends BaseMessage
    case class MarkAsUnreadFeed(num: Long) extends BaseMessage

    case class AddFeeds(sourceId: Long, xs: Vector[Feed]) extends BaseMessage


    case class Latest(count: Long) extends BaseMessage
    case class ExtractFeedsForSource(sourceId: Long) extends BaseMessage

    // util:
    case class UrlIsUniq(url: String, id: Option[Long] = None) extends BaseMessage
    case class NameIsUniq(name: String, id: Option[Long] = None) extends BaseMessage
    case class FeedCount(read: Boolean = false) extends BaseMessage

    case class SetState(sourceId: Long, state: SourceState) extends BaseMessage
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
    case object GetPluginList extends BaseMessage
  }

  object util {
    case object Start
    case object Update extends BaseMessage
    case object Updated
    case class UpdateMe(who: ActorRef)
    case class UpdateOne(num: Long) extends BaseMessage

    case class SourceDeleted(source: Source)
    case class SourceLastUpdate(sourceId: Long)
    case class FeedContentUpdate(feedId: Long, content: String)

    case class NewSource(source: Source)
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
    case class SourceAdded(source: SourceForFrontend)
    case class SourceUpdated(source: SourceForFrontend)
    case class NewFeeds(xs: Vector[Feed])
  }


}
