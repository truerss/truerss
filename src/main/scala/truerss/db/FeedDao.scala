package truerss.db

import com.github.truerss.base.Entry
import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.CurrentDriver
import zio.Task

import scala.concurrent.{ExecutionContext, Future}

class FeedDao(val db: DatabaseDef)(implicit
                                   val ec: ExecutionContext,
                                   driver: CurrentDriver
) {

  import JdbcTaskSupport._
  import driver.profile.api._
  import driver.query.{feeds, bySource, byFeed, FeedsTQExt, FeedsQExt}
  import FeedDao._

  private type FPage = Task[(Seq[Feed], Int)]

  def findUnread(sourceId: Long): Task[Seq[Feed]] = {
    db.go {
      bySource(sourceId)
        .unreadOnly
        .sortBy(_.publishedDate.desc)
        .result
    }
  }

  def feedBySourceCount(read: Boolean): Task[Seq[(Long, Int)]] = {
    db.go {
      feeds
        .filter(_.read === read)
        .groupBy(_.sourceId)
        .map {
          case (sourceId, xs) => sourceId -> xs.size
        }.result
    }
  }

  def feedCountBySourceId(sourceId: Long, unreadOnly: Boolean): Task[Int] = {
    db.go {
      bySource(sourceId)
        .filter(_.read inSet getReadValues(unreadOnly))
        .length.result
    }
  }

  def deleteFeedsBySource(sourceId: Long): Task[Int] = {
    db.go { bySource(sourceId).delete }
  }

  def markAll: Task[Int] = {
    db.go {
      feeds
        .unreadOnly
        .map(_.read)
        .update(true)
    }
  }

  def markBySource(sourceId: Long): Task[Int] = {
    db.go {
      bySource(sourceId)
        .map(f => f.read)
        .update(true)
    }
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

  def findOne(feedId: Long): Task[Option[Feed]] = {
    db.go {
      byFeed(feedId).take(1).result
    }.map(_.headOption)
  }

  def findSingle(feedId: Long): Task[Feed] = {
    db.go {
      byFeed(feedId).take(1).result
    }.map(_.head)
  }

  def modifyFav(feedId: Long, fav: Boolean): Task[Int] = {
    db.go {
      byFeed(feedId).map(e => e.favorite).update(fav)
    }
  }

  def modifyRead(feedId: Long, read: Boolean): Task[Int] = {
    db.go {
      byFeed(feedId).map(e => e.read).update(read)
    }
  }

  def updateContent(feedId: Long, content: String): Task[Int] = {
    db.go {
      byFeed(feedId).map(_.content).update(content)
    }
  }

  def insert(feed: Feed): Task[Long] = {
    db.go {
      (feeds returning feeds.map(_.id)) += feed
    }
  }
  def insertMany(xs: Iterable[Feed]) = {
    db.go { feeds ++= xs }
  }

  def updateByUrl(feed: Feed): Task[Int] = {
    db.go {
      feeds.filter(_.url === feed.url)
        .map(f => (f.title, f.author, f.publishedDate,
          f.description, f.normalized, f.content, f.read))
        .update((feed.title, feed.author, feed.publishedDate,
          feed.description.orNull,
          feed.normalized, feed.content.orNull, false))
    }
  }

  private def updateByUrl(feeds: Iterable[Feed]): Task[Iterable[Int]] = {
    Task.collectAll(feeds.map { x => updateByUrl(x) })
  }

  def findBySource(sourceId: Long): Task[Seq[Feed]] = {
    db.go {
      bySource(sourceId).result
    }
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
      // return new urls
      ???
    }
  }

  private def getReadValues(unreadOnly: Boolean): Vector[Boolean] = {
    if (unreadOnly) {
      Vector(false)
    } else {
      Vector(true, false)
    }
  }

  private def fetchPage(q: Query[driver.Feeds, Feed, Seq], offset: Int, limit: Int): FPage = {
    val act = for {
      resources <- q.drop(offset).take(limit).result
      total <- q.length.result
    } yield {
      (resources, total)
    }
    db.go(act)
  }


}

object FeedDao {
  import truerss.util.Util._

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