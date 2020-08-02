package truerss.services

import com.github.truerss.base.Entry
import truerss.db.DbLayer
import truerss.dto.FeedDto
import truerss.db.Feed
import zio.Task

class FeedsService(private val dbLayer: DbLayer) {

  import truerss.util.FeedSourceDtoModelImplicits._
  import FeedsService._
  import dbLayer.feedDao

  def findOne(feedId: Long): Task[FeedDto] = {
    dbLayer.feedDao.findOne(feedId).map(_.toDto)
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

  def changeRead(feedId: Long, readFlag: Boolean): Task[Unit] = {
    for {
      _ <- findOne(feedId)
      _ <- feedDao.modifyRead(feedId, read = readFlag)
    } yield ()
  }

  def changeFav(feedId: Long, favFlag: Boolean): Task[Unit] = {
    for {
      _ <- findOne(feedId)
      _ <- feedDao.modifyFav(feedId, fav = favFlag)
    } yield ()
  }

  def registerNewFeeds(sourceId: Long, feeds: Iterable[Entry]): Task[Iterable[FeedDto]] = {
    feedDao.mergeFeeds(sourceId, feeds).map { xs => xs.map(_.toDto) }
  }

  def updateContent(feedId: Long, content: String): Task[Unit] = {
    feedDao.updateContent(feedId, content).map(_ => ())
  }

}

object FeedsService {
  import truerss.util.FeedSourceDtoModelImplicits._

  type Page = (Vector[FeedDto], Int)
  type FPage = Task[Page]

  def toPage(tmp: (Seq[Feed], Int)): truerss.dto.Page[FeedDto] = {
    truerss.dto.Page(tmp._2, tmp._1.map(_.toDto))
  }

  def convert(xs: Seq[Feed]): Vector[FeedDto] = {
    xs.map(_.toDto).toVector
  }
}