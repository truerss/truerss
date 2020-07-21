package truerss.db

import com.github.truerss.base.Entry
import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.CurrentDriver
import truerss.services.NotFoundError
import zio.{IO, Task, ZIO}


class FeedDao(val db: DatabaseDef)(implicit driver: CurrentDriver) {

  import JdbcTaskSupport._
  import driver.profile.api._
  import driver.query.{feeds, bySource, byFeed, FeedsTQExt, FeedsQExt}
  import FeedDao._

  private type FPage = Task[(Seq[Feed], Int)]

  def findUnread(sourceId: Long): Task[Seq[Feed]] = {
    bySource(sourceId)
      .unreadOnly
      .sortBy(_.publishedDate.desc)
      .result ~> db
  }

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

  def lastN(offset: Int, limit: Int): FPage = {
    fetchPage(feeds.unreadOnly, offset, limit)
  }

  def pageForSource(sourceId: Long, unreadOnly: Boolean, offset: Int, limit: Int): FPage = {
    val f = bySource(sourceId)
      .filter(_.read inSet getReadValues(unreadOnly))
    fetchPage(f, offset, limit)
  }

  def favorites(offset: Int, limit: Int): FPage = {
    fetchPage(
      feeds.isFavorite, offset, limit
    )
  }

  def findOne(feedId: Long): IO[NotFoundError, Feed] = {
    val t = for {
      feedOpt <- db.go {
        byFeed(feedId).take(1).result
      }.map(_.headOption)
      feed <- ZIO.fromOption(feedOpt)
    } yield feed
    t.mapError {
      case None =>
        NotFoundError(feedId)
    }
//    db.go {
//      byFeed(feedId).take(1).result
//    }.map(_.headOption).map {
//      case Some(feed) => IO.succeed(feed)
//      case None => IO.fail(NotFoundError(feedId))
//    }.flatten
//    db.go {
//      byFeed(feedId).take(1).result
//    }.map(_.head).mapError { _ =>
//      NotFoundError(feedId)
//    }
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

  def search(inFavorites: Boolean, query: String, offset: Int, limit: Int): FPage = {
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

  def mergeFeeds(sourceId: Long, xs: Iterable[Entry]): Task[Seq[Feed]] = {
    val urls = xs.map(_.url)
    for {
      inDb <- db.go(bySource(sourceId).filter(_.url inSet urls).result)
      calc = calculate(sourceId, xs, inDb)
      _ <- insertMany(calc.feedsToInsert)
      _ <- updateByUrl(calc.feedsToUpdateByUrl)
    } yield {
      // return by new urls
      ???
    }
  }

  private def fetchPage(q: Query[driver.Feeds, Feed, Seq], offset: Int, limit: Int): FPage = {
    for {
      resources <- db.go(q.drop(offset).take(limit).result)
      total <- db.go(q.length.result)
    } yield {
      (resources, total)
    }
  }

}

object FeedDao {
  import truerss.util.Util._

  def getReadValues(unreadOnly: Boolean): Vector[Boolean] = {
    if (unreadOnly) {
      Vector(false)
    } else {
      Vector(true, false)
    }
  }

  def calculate(sourceId: Long, xs: Iterable[Entry], inDb: Seq[Feed]): FeedCalc = {
    val inDbMap = inDb.map(f => f.url -> f).toMap
    val (forceUpdateXs, updateXs) = xs.partition(_.forceUpdate)

    val (feedsToUpdateByUrl, feedsToInsert) = forceUpdateXs
      .map(_.toFeed(sourceId))
      .partition(f => inDbMap.contains(f.url))

    val updateXsMap = updateXs.map(_.toFeed(sourceId)).map(f => f.url -> f).toMap
    val inDbUrls = inDbMap.keySet
    val fromNetwork = updateXsMap.keySet
    val newFeeds = (fromNetwork diff inDbUrls).flatMap(updateXsMap.get)
    FeedCalc(
      feedsToUpdateByUrl = feedsToUpdateByUrl,
      feedsToInsert = (feedsToInsert ++ newFeeds).groupBy(_.url).values.flatten
    )
  }
}

case class FeedCalc(
                     feedsToUpdateByUrl: Iterable[Feed],
                     feedsToInsert: Iterable[Feed]
                   )