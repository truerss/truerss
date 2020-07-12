package truerss.services

import com.github.truerss.base.Entry
import truerss.db.DbLayer
import truerss.dto.FeedDto
import truerss.db.Feed
import truerss.services.management.FeedSourceDtoModelImplicits
import truerss.util.syntax
import zio.Task

import scala.concurrent.{ExecutionContext, Future}

class FeedsService(dbLayer: DbLayer)(implicit val ec: ExecutionContext) {

  import FeedSourceDtoModelImplicits._
  import FeedsService._
  import syntax.future._
  import dbLayer.feedDao

  def findSingle(feedId: Long): Task[FeedDto] = {
    dbLayer.feedDao.findSingle(feedId).map(_.toDto)
  }

  def findOne(feedId: Long): Task[Option[FeedDto]] = {
    dbLayer.feedDao.findOne(feedId).map(x => x.map(_.toDto))
  }

  def findUnread(sourceId: Long): Task[Vector[FeedDto]] = {
    feedDao.findUnread(sourceId).map(convert)
  }

  def findBySource(sourceId: Long, unreadOnly: Boolean, offset: Int, limit: Int): Future[(Vector[FeedDto], Int)] = {
    feedDao.pageForSource(sourceId, unreadOnly, offset, limit).map(toPage)
  }

  def latest(offset: Int, limit: Int): FPage = {
    Task.fromFuture { implicit ec => feedDao.lastN(offset, limit) }.map(toPage)
  }

  def favorites(offset: Int, limit: Int): FPage = {
    Task.fromFuture { implicit ec => feedDao.favorites(offset, limit)}.map(toPage)
  }

  def markAllAsRead: Task[Int] = {
    feedDao.markAll
  }

  def changeRead(feedId: Long, readFlag: Boolean): Task[Option[FeedDto]] = {
    val t = for {
      feed <- dbLayer.feedDao.findOne(feedId)
      _ <- feedDao.modifyRead(feedId, read = readFlag)
    } yield feed.map(_.toDto.copy(read = readFlag))
    t
  }

  def changeFav(feedId: Long, favFlag: Boolean): Task[Option[FeedDto]] = {
    for {
      feed <- dbLayer.feedDao.findOne(feedId)
      _ <- feedDao.modifyFav(feedId, fav = favFlag)
    } yield feed.map(_.toDto.copy(favorite = favFlag))
  }

  def registerNewFeeds(sourceId: Long, feeds: Vector[Entry]): Future[Vector[FeedDto]] = {
    feedDao.mergeFeeds(sourceId, feeds)
      .map(_.toVector)
      .map(xs => xs.map(_.toDto))
  }

  def updateContent(feedId: Long, content: String): Task[Unit] = {
    feedDao.updateContent(feedId, content).map(_ => ())
  }

  private def findAndModify(feedId: Long)(f: Feed => Task[Feed]): Task[Option[FeedDto]] = {
    feedDao.findOne(feedId).flatMap {
      case Some(x) => f(x).map(x => Option(x.toDto))
      case _ => Task.succeed(None)
    }
  }

}

object FeedsService {
  import FeedSourceDtoModelImplicits._

  type Page = (Vector[FeedDto], Int)
  type FPage = Task[Page]

  def toPage(tmp: (Seq[Feed], Int)): Page = {
    (tmp._1.map(_.toDto).toVector, tmp._2)
  }

  def convert(xs: Seq[Feed]): Vector[FeedDto] = {
    xs.map(_.toDto).toVector
  }
}