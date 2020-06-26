package truerss.services

import truerss.db.DbLayer
import truerss.dto.FeedDto
import truerss.db.Feed
import truerss.services.management.FeedSourceDtoModelImplicits

import scala.concurrent.{ExecutionContext, Future}

class FeedsService(dbLayer: DbLayer)(implicit val ec: ExecutionContext) {

  import FeedSourceDtoModelImplicits._

  type FPage = Future[(Vector[FeedDto], Int)]

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
    fetchPage(
      dbLayer.feedDao.pageForSource(sourceId, unreadOnly, offset, limit),
      dbLayer.feedDao.feedCountBySourceId(sourceId, unreadOnly)
    )
  }

  def latest(offset: Int, limit: Int): FPage = {
    fetchPage(
      dbLayer.feedDao.lastN(offset, limit),
      dbLayer.feedDao.lastNCount()
    )
  }

  def favorites(offset: Int, limit: Int): FPage = {
    fetchPage(
      dbLayer.feedDao.favorites(offset, limit),
      dbLayer.feedDao.favoritesCount()
    )
  }

  private def fetchPage(feedsF: Future[Seq[Feed]], totalF: Future[Int]): FPage = {
    for {
      feeds <- feedsF
      total <- totalF
    } yield (convert(feeds), total)
  }

  private def convert(xs: Seq[Feed]): Vector[FeedDto] = {
    xs.map(_.toDto).toVector
  }

  private def findAndModify(feedId: Long)(f: Option[Feed] => Future[Option[Feed]]): Future[Option[FeedDto]] = {
    dbLayer.feedDao.findOne(feedId).flatMap(f).map { x => x.map(_.toDto) }
  }


}
