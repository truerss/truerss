package truerss.db

import com.github.truerss.base.Entry
import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.CurrentDriver
import truerss.services.NotFoundError
import truerss.util.FeedsMerger
import zio.{IO, Task}

class FeedsDao(val db: DatabaseDef)(implicit driver: CurrentDriver) {

  import FeedsDao._
  import FeedsMerger._
  import JdbcTaskSupport._
  import driver.profile.api._
  import driver.query.{FeedsQExt, FeedsTQExt, byFeed, bySource, feeds}

  private type TPage = Task[(Seq[Feed], Int)]

  def feedBySourceCount(read: Boolean): Task[Seq[(Long, Int)]] = {
    feeds
      .filter(_.read === read)
      .groupBy(_.sourceId)
      .map {
        case (sourceId, xs) => sourceId -> xs.size
      }.result ~> db
  }

  def feedCountBySourceId(sourceId: Long, unreadOnly: Boolean): Task[Int] = {
    bySource(sourceId)
      .filter(_.read inSet getReadValues(unreadOnly))
      .length.result ~> db
  }

  def deleteFeedsBySource(sourceId: Long): Task[Int] = {
    bySource(sourceId).delete ~> db
  }

  def markAll: Task[Int] = {
    feeds
      .unreadOnly
      .map(_.read)
      .update(true) ~> db
  }

  def markBySource(sourceId: Long): Task[Int] = {
    bySource(sourceId)
      .map(f => f.read)
      .update(true) ~> db
  }

  def lastN(offset: Int, limit: Int): TPage = {
    fetchPage(feeds.unreadOnly, offset, limit)
  }

  def pageForSource(sourceId: Long, unreadOnly: Boolean, offset: Int, limit: Int): TPage = {
    val f = bySource(sourceId)
      .filter(_.read inSet getReadValues(unreadOnly))
    fetchPage(f, offset, limit)
  }

  def favorites(offset: Int, limit: Int): TPage = {
    fetchPage(
      feeds.isFavorite, offset, limit
    )
  }

  def findOne(feedId: Long): IO[NotFoundError, Feed] = {
    byFeed(feedId).take(1).result.headOption ~> db <~ feedId
  }

  def modifyFav(feedId: Long, fav: Boolean): Task[Int] = {
    byFeed(feedId).map(e => e.favorite).update(fav) ~> db
  }

  def modifyRead(feedId: Long, read: Boolean): Task[Int] = {
    byFeed(feedId).map(e => e.read).update(read) ~> db
  }

  def updateContent(feedId: Long, content: String): Task[Int] = {
    byFeed(feedId).map(_.content).update(content) ~> db
  }

  def insert(feed: Feed): Task[Long] = {
    ((feeds returning feeds.map(_.id)) += feed) ~> db
  }

  def insertMany(xs: Iterable[Feed]) = {
    (feeds ++= xs) ~> db
  }

  def updateByUrl(feed: Feed): Task[Int] = {
    feeds.filter(_.url === feed.url)
      .map(f => (f.title, f.author, f.publishedDate,
        f.description, f.normalized, f.content, f.read))
      .update((feed.title, feed.author, feed.publishedDate,
        feed.description.orNull,
        feed.normalized, feed.content.orNull, false)) ~> db
  }

  private def updateByUrl(feeds: Iterable[Feed]): Task[Iterable[Int]] = {
    Task.collectAll(feeds.map { x => updateByUrl(x) })
  }

  def findBySource(sourceId: Long): Task[Seq[Feed]] = {
    bySource(sourceId).result ~> db
  }

  def search(inFavorites: Boolean, query: String, offset: Int, limit: Int): TPage = {
    val exp = s"%$query%"
    val f = feeds
      .filter{x => (x.title like exp) || (x.normalized like exp) || (x.url like exp)}

    val q = if (inFavorites) {
      f.filter(x => x.favorite === true)
    } else {
      f
    }
    fetchPage(q, offset, limit)
  }

  private def findByUrls(sourceId: Long, urls: Iterable[String]): Task[Iterable[Feed]] = {
    db.go(bySource(sourceId).filter(_.url inSet urls).result)
  }

  def mergeFeeds(sourceId: Long, xs: Iterable[Entry]): Task[Iterable[Feed]] = {
    for {
      inDb <- findByUrls(sourceId, xs.map(_.url))
      calc = calculate(sourceId, xs, inDb)
      _ <- insertMany(calc.feedsToInsert)
      _ <- updateByUrl(calc.feedsToUpdateByUrl)
      feeds <- findByUrls(sourceId, calc.feedsToInsert.map(_.url))
    } yield {
      feeds ++ calc.feedsToUpdateByUrl
    }
  }

  private def fetchPage(q: Query[driver.Feeds, Feed, Seq], offset: Int, limit: Int): TPage = {
    for {
      resources <- db.go(q.drop(offset).take(limit).result)
      total <- db.go(q.length.result)
    } yield {
      (resources, total)
    }
  }

}

object FeedsDao {
  def getReadValues(unreadOnly: Boolean): Vector[Boolean] = {
    if (unreadOnly) {
      Vector(false)
    } else {
      Vector(true, false)
    }
  }
}
