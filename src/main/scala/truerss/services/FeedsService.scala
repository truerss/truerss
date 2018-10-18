package truerss.services

import truerss.db.DbLayer
import truerss.dto.FeedDto
import truerss.models.Feed
import truerss.services.actors.DtoModelImplicits

import scala.concurrent.{ExecutionContext, Future}

class FeedsService(dbLayer: DbLayer)(implicit val ec: ExecutionContext) {

  import DtoModelImplicits._

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
      dbLayer.feedDao.modifyRead(feedId, true).map { _ =>
        feed.map(f => f.copy(read = true))
      }
    }
  }

  def markAsUnread(feedId: Long): Future[Option[FeedDto]] = {
    findAndModify(feedId) {feed =>
      dbLayer.feedDao.modifyRead(feedId, false).map { _ =>
        feed.map(f => f.copy(read = false))
      }
    }
  }

  def findUnread(sourceId: Long): Future[Vector[FeedDto]] = {
    dbLayer.feedDao.findUnread(sourceId).map(t)
  }

  def findBySource(sourceId: Long, from: Int, limit: Int): Future[(Vector[FeedDto], Int)] = {
    for {
      feeds <- dbLayer.feedDao.pageForSource(sourceId, from, limit)
      total <- dbLayer.feedDao.feedCountBySourceId(sourceId)
    } yield (t(feeds), total)
  }

  def latest(count: Int): Future[Vector[FeedDto]] = {
    dbLayer.feedDao.lastN(count).map(t)
  }

  def favorites: Future[Vector[FeedDto]] = {
    dbLayer.feedDao.favorites.map(t)
  }


  private def t(xs: Seq[Feed]): Vector[FeedDto] = {
    xs.map(_.toDto).toVector
  }


  private def findAndModify(feedId: Long)(f: Option[Feed] => Future[Option[Feed]]): Future[Option[FeedDto]] = {
    dbLayer.feedDao.findOne(feedId).flatMap(f).map { x => x.map(_.toDto) }
  }


}
