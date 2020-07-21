package truerss.services

import com.github.truerss.base.Entry
import truerss.db.DbLayer
import truerss.dto.FeedDto
import truerss.db.Feed
import truerss.services.management.FeedSourceDtoModelImplicits
import truerss.util.syntax
import zio.Task

class FeedsService(dbLayer: DbLayer) {

  import FeedSourceDtoModelImplicits._
  import FeedsService._
  import syntax.future._
  import dbLayer.feedDao

  def findOne(feedId: Long): Task[FeedDto] = {
    dbLayer.feedDao.findOne(feedId).map(_.toDto)
  }

  // todo pagination too
  def findUnread(sourceId: Long): Task[Vector[FeedDto]] = {
    feedDao.findUnread(sourceId).map(convert)
  }

  def findBySource(sourceId: Long, unreadOnly: Boolean, offset: Int, limit: Int): Task[truerss.dto.Page[FeedDto]] = {
    feedDao.pageForSource(sourceId, unreadOnly, offset, limit).map(toPage)
  }

  def latest(offset: Int, limit: Int): Task[truerss.dto.Page[FeedDto]] = {
    feedDao.lastN(offset, limit).map(toPage)
  }

  def favorites(offset: Int, limit: Int): Task[truerss.dto.Page[FeedDto]] = {
    feedDao.favorites(offset, limit).map(toPage)
  }

  def markAllAsRead: Task[Unit] = {
    feedDao.markAll.map(_ => ())
  }

  def changeRead(feedId: Long, readFlag: Boolean): Task[FeedDto] = {
    val t = for {
      feed <- findOne(feedId)
      _ <- feedDao.modifyRead(feedId, read = readFlag)
    } yield feed.copy(read = readFlag)
    t
  }

  def changeFav(feedId: Long, favFlag: Boolean): Task[FeedDto] = {
    for {
      feed <- findOne(feedId)
      _ <- feedDao.modifyFav(feedId, fav = favFlag)
    } yield feed.copy(favorite = favFlag)
  }

  def registerNewFeeds(sourceId: Long, feeds: Vector[Entry]): Task[Vector[FeedDto]] = {
    feedDao.mergeFeeds(sourceId, feeds)
      .map(_.toVector)
      .map(xs => xs.map(_.toDto))
  }

  def updateContent(feedId: Long, content: String): Task[Unit] = {
    feedDao.updateContent(feedId, content).map(_ => ())
  }

}

object FeedsService {
  import FeedSourceDtoModelImplicits._

  type Page = (Vector[FeedDto], Int)
  type FPage = Task[Page]

  def toPage(tmp: (Seq[Feed], Int)): truerss.dto.Page[FeedDto] = {
    truerss.dto.Page(tmp._2, tmp._1.map(_.toDto))
  }

  def convert(xs: Seq[Feed]): Vector[FeedDto] = {
    xs.map(_.toDto).toVector
  }
}