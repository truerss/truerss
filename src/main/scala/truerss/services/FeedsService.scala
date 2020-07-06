package truerss.services

import com.github.truerss.base.Entry
import truerss.db.DbLayer
import truerss.dto.FeedDto
import truerss.db.Feed
import truerss.services.management.FeedSourceDtoModelImplicits
import truerss.util.syntax

import scala.concurrent.{ExecutionContext, Future}

class FeedsService(dbLayer: DbLayer)(implicit val ec: ExecutionContext) {

  import FeedSourceDtoModelImplicits._
  import FeedsService._
  import syntax.future._
  import dbLayer.feedDao

  def findOne(feedId: Long): Future[Option[FeedDto]] = {
    findAndModify(feedId)(_.toF)
  }

  def findUnread(sourceId: Long): Future[Vector[FeedDto]] = {
    feedDao.findUnread(sourceId).map(convert)
  }

  def findBySource(sourceId: Long, unreadOnly: Boolean, offset: Int, limit: Int): Future[(Vector[FeedDto], Int)] = {
    feedDao.pageForSource(sourceId, unreadOnly, offset, limit).map(toPage)
  }

  def latest(offset: Int, limit: Int): FPage = {
    feedDao.lastN(offset, limit).map(toPage)
  }

  def favorites(offset: Int, limit: Int): FPage = {
    feedDao.favorites(offset, limit).map(toPage)
  }

  def markAllAsRead: Future[Int] = {
    feedDao.markAll
  }

  def changeRead(feedId: Long, readFlag: Boolean): Future[Option[FeedDto]] = {
    findAndModify(feedId) { feed =>
      feedDao.modifyRead(feedId, read = readFlag).map { _ =>
        feed.copy(read = readFlag)
      }
    }
  }

  def changeFav(feedId: Long, favFlag: Boolean): Future[Option[FeedDto]] = {
    findAndModify(feedId) { feed =>
      feedDao.modifyFav(feedId, fav = favFlag).map { _ =>
        feed.copy(favorite = favFlag)
      }
    }
  }

  def registerNewFeeds(sourceId: Long, feeds: Vector[Entry]): Future[Vector[FeedDto]] = {
    feedDao.mergeFeeds(sourceId, feeds)
      .map(_.toVector)
      .map(xs => xs.map(_.toDto))
  }

  def updateContent(feedId: Long, content: String): Future[Unit] = {
    feedDao.updateContent(feedId, content).map(_ => ())
  }

  private def findAndModify(feedId: Long)(f: Feed => Future[Feed]): Future[Option[FeedDto]] = {
    feedDao.findOne(feedId).flatMap {
      case Some(x) => f(x).map(x => Option(x.toDto))
      case _ => Future.successful(None)
    }
  }

}

object FeedsService {
  import FeedSourceDtoModelImplicits._

  type Page = (Vector[FeedDto], Int)
  type FPage = Future[Page]

  def toPage(tmp: (Seq[Feed], Int)): Page = {
    (tmp._1.map(_.toDto).toVector, tmp._2)
  }

  def convert(xs: Seq[Feed]): Vector[FeedDto] = {
    xs.map(_.toDto).toVector
  }
}