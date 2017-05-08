package truerss.services.actors

import akka.actor.Props
import akka.pattern.pipe
import truerss.api.{ModelResponse, ModelsResponse}
import truerss.db.DbLayer
import truerss.models.Feed
import truerss.services.PublishPluginActor
import truerss.util.Util.ResponseHelpers

/**
  * Created by mike on 4.5.17.
  */
class FeedsManagementActor(dbLayer: DbLayer) extends CommonActor {

  import FeedsManagementActor._
  import context.dispatcher

  override def defaultHandler: Receive = {
    case MarkAll =>
      val result = dbLayer.feedDao.markAll
        .map(_.toLong).map { _ => ok }
      result pipeTo sender

    case MarkFeed(feedId) =>
      val result = dbLayer.feedDao.findOne(feedId).map { feed =>
        dbLayer.feedDao.modifyFav(feedId, fav = true)
        feed.map(_.mark(true))
      }.map {
        case Some(feed) =>
          stream.publish(PublishPluginActor.PublishEvent(feed))
          ModelResponse(feed)
        case None =>
          feedNotFound
      }
      result pipeTo sender

    case UnmarkFeed(feedId) =>
      val result = dbLayer.feedDao.findOne(feedId).map { feed =>
        dbLayer.feedDao.modifyFav(feedId, fav = false)
        feed.map(_.mark(false))
      }.map(feedHandler)

      result pipeTo sender


    case MarkAsReadFeed(feedId) =>
      val result = dbLayer.feedDao.findOne(feedId).map { feed =>
        dbLayer.feedDao.modifyRead(feedId, true)
        feed.map(f => f.copy(read = true))
      }.map(feedHandler)

      result pipeTo sender

    case MarkAsUnreadFeed(feedId) =>
      val result = dbLayer.feedDao.findOne(feedId).map { feed =>
        dbLayer.feedDao.modifyRead(feedId, false)
        feed.map(f => f.copy(read = false))
      }
      result pipeTo sender

    case Unread(sourceId) =>
      val result = dbLayer.feedDao.findUnread(sourceId)
        .map(_.toVector)
        .map(ModelsResponse(_))

      result pipeTo sender

    case ExtractFeedsForSource(sourceId, from, limit) =>
      val result = for {
        feeds <- dbLayer.feedDao.pageForSource(sourceId, from, limit)
          .map(_.toVector)
        total <- dbLayer.feedDao.feedCountBySourceId(sourceId)
      } yield {
        ModelsResponse(feeds, total)
      }

      result pipeTo sender

    case Latest(count) =>
      val result = dbLayer.feedDao.lastN(count)
        .map(_.toVector).map(ModelsResponse(_))

      result pipeTo sender

    case Favorites =>
      val result = dbLayer.feedDao.favorites
        .map(_.toVector).map(ModelsResponse(_))
      result pipeTo sender

    case msg: GetFeed =>
      context.actorOf(GetFeedActor.props(dbLayer, context.parent)) forward msg

  }


}

object FeedsManagementActor {

  def props(dbLayer: DbLayer) = {
    Props(classOf[FeedsManagementActor], dbLayer)
  }

  def feedHandler(feed: Option[Feed]) = {
    feed match {
      case Some(f) => ModelResponse(f)
      case None => ResponseHelpers.feedNotFound
    }
  }

  sealed trait FeedsMessage
  case object MarkAll extends FeedsMessage

  case object Favorites extends FeedsMessage
  case class GetFeed(num: Long) extends FeedsMessage
  case class MarkFeed(num: Long) extends FeedsMessage
  case class UnmarkFeed(num: Long) extends FeedsMessage
  case class MarkAsReadFeed(num: Long) extends FeedsMessage
  case class MarkAsUnreadFeed(num: Long) extends FeedsMessage
  case class Latest(count: Long) extends FeedsMessage
  case class ExtractFeedsForSource(sourceId: Long,
                                   from: Int = 0,
                                   limit: Int = 100) extends FeedsMessage
  case class Unread(sourceId: Long) extends FeedsMessage

}