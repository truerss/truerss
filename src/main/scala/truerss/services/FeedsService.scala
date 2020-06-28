package truerss.services

import com.github.truerss.base.Entry
import truerss.db.DbLayer
import truerss.dto.FeedDto
import truerss.db.Feed
import truerss.services.management.FeedSourceDtoModelImplicits

import scala.concurrent.{ExecutionContext, Future}

class FeedsService(dbLayer: DbLayer)(implicit val ec: ExecutionContext) {

  import FeedSourceDtoModelImplicits._
  import FeedsService._

  def findOne(feedId: Long): Future[Option[FeedDto]] = {
    dbLayer.feedDao.findOne(feedId).map { x => x.map(_.toDto) }
  }

  def markAllAsRead: Future[Int] = {
    dbLayer.feedDao.markAll
  }

  def addToFavorites(feedId: Long): Future[Option[FeedDto]] = {
    findAndModify(feedId) { feed =>
      dbLayer.feedDao.modifyFav(feedId, fav = true).map { _ =>
        feed.map(_.mark(true))
      }
    }
  }

  def removeFromFavorites(feedId: Long): Future[Option[FeedDto]] = {
    findAndModify(feedId) { feed =>
      dbLayer.feedDao.modifyFav(feedId, fav = false).map { _ =>
        feed.map(_.mark(false))
      }
    }
  }

  def markAsRead(feedId: Long): Future[Option[FeedDto]] = {
    findAndModify(feedId) { feed =>
      dbLayer.feedDao.modifyRead(feedId, read = true).map { _ =>
        feed.map(f => f.copy(read = true))
      }
    }
  }

  def markAsUnread(feedId: Long): Future[Option[FeedDto]] = {
    findAndModify(feedId) {feed =>
      dbLayer.feedDao.modifyRead(feedId, read = false).map { _ =>
        feed.map(f => f.copy(read = false))
      }
    }
  }

  def findUnread(sourceId: Long): Future[Vector[FeedDto]] = {
    dbLayer.feedDao.findUnread(sourceId).map(convert)
  }

  def findBySource(sourceId: Long, unreadOnly: Boolean, offset: Int, limit: Int): Future[(Vector[FeedDto], Int)] = {
    dbLayer.feedDao.pageForSource(sourceId, unreadOnly, offset, limit).map(toPage)
  }

  def latest(offset: Int, limit: Int): FPage = {
    dbLayer.feedDao.lastN(offset, limit).map(toPage)
  }

  def favorites(offset: Int, limit: Int): FPage = {
    dbLayer.feedDao.favorites(offset, limit).map(toPage)
  }

  def registerNewFeeds(sourceId: Long, feeds: Vector[Entry]): Future[Vector[FeedDto]] = {
    dbLayer.feedDao.mergeFeeds(sourceId, feeds)
      .map(_.toVector)
      .map(xs => xs.map(_.toDto))
  }

  def updateContent(feedId: Long, content: String): Unit = {
    dbLayer.feedDao.updateContent(feedId, content).foreach(identity)
  }

  private def findAndModify(feedId: Long)(f: Option[Feed] => Future[Option[Feed]]): Future[Option[FeedDto]] = {
    dbLayer.feedDao.findOne(feedId).flatMap(f).map { x => x.map(_.toDto) }
  }
}

object FeedsService {
  import FeedSourceDtoModelImplicits._

  type FPage = Future[(Vector[FeedDto], Int)]

  def toPage(tmp: (Seq[Feed], Int)) = {
    (tmp._1.map(_.toDto).toVector, tmp._2)
  }

  def convert(xs: Seq[Feed]): Vector[FeedDto] = {
    xs.map(_.toDto).toVector
  }
}