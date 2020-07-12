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

  import driver.profile.api._
  import driver.query.{feeds, bySource, byFeed, FeedsTQExt, FeedsQExt}
  import truerss.util.Util._

  private type FPage = Future[(Seq[Feed], Int)]

  def findUnread(sourceId: Long): Task[Seq[Feed]] = {
    ft {
      db.run {
        bySource(sourceId)
          .unreadOnly
          .sortBy(_.publishedDate.desc)
          .result
      }
    }
  }

  def feedBySourceCount(read: Boolean): Task[Seq[(Long, Int)]] = {
    ft {
      db.run {
        feeds
          .filter(_.read === read)
          .groupBy(_.sourceId)
          .map {
            case (sourceId, xs) => sourceId -> xs.size
          }.result
      }
    }
  }

  def feedCountBySourceId(sourceId: Long, unreadOnly: Boolean): Task[Int] = {
    ft {
      db.run {
        bySource(sourceId)
          .filter(_.read inSet getReadValues(unreadOnly))
          .length.result
      }
    }
  }

  def deleteFeedsBySource(sourceId: Long): Task[Int] = {
    ft {
      db.run {
        bySource(sourceId).delete
      }
    }
  }

  def markAll: Task[Int] = {
    ft {
      db.run {
        feeds
          .unreadOnly
          .map(_.read)
          .update(true)
      }
    }
  }

  def markBySource(sourceId: Long): Task[Int] = {
    ft {
      db.run {
        bySource(sourceId)
          .map(f => f.read)
          .update(true)
      }
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
    ft {
      db.run {
        byFeed(feedId).take(1).result
      }.map(_.headOption)
    }
  }

  def findSingle(feedId: Long): Task[Feed] = {
    ft {
      db.run {
        byFeed(feedId).take(1).result
      }.map(_.head)
    }
  }

  def modifyFav(feedId: Long, fav: Boolean): Task[Int] = {
    ft {
      db.run {
        byFeed(feedId).map(e => e.favorite).update(fav)
      }
    }
  }

  def modifyRead(feedId: Long, read: Boolean): Task[Int] = {
    ft {
      db.run {
        byFeed(feedId).map(e => e.read).update(read)
      }
    }
  }

  def updateContent(feedId: Long, content: String): Task[Int] = {
    ft {
      db.run {
        byFeed(feedId).map(_.content).update(content)
      }
    }
  }

  def insert(feed: Feed): Task[Long] = {
    ft {
      db.run {
        (feeds returning feeds.map(_.id)) += feed
      }
    }
  }

  def updateByUrl(feed: Feed): Task[Int] = {
    ft {
      db.run {
        feeds.filter(_.url === feed.url)
          .map(f => (f.title, f.author, f.publishedDate,
            f.description, f.normalized, f.content, f.read))
          .update((feed.title, feed.author, feed.publishedDate,
            feed.description.orNull,
            feed.normalized, feed.content.orNull, false))
      }
    }
  }

  def findBySource(sourceId: Long): Task[Seq[Feed]] = {
    ft {
      db.run {
        bySource(sourceId).result
      }
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

  def mergeFeeds(sourceId: Long, xs: Iterable[Entry]): Future[Seq[Feed]] = {
    val urls = xs.map(_.url)
    val q = bySource(sourceId).filter(_.url inSet urls)
    db.run {
      q.result
    }.flatMap { inDb =>
      val inDbMap = inDb.map(f => f.url -> f).toMap
      val (forceUpdateXs, updateXs) = xs.partition(_.forceUpdate)
      db.run {
        forceUpdateXs.map { entry =>
          val feed = entry.toFeed(sourceId)
          inDbMap.get(feed.url) match {
            case Some(_) =>
              updateByUrl(feed)
            case None =>
              insert(feed)
          }
        }

        val updateXsMap = updateXs.map(_.toFeed(sourceId)).map(f => f.url -> f).toMap
        val inDbUrls = inDbMap.keySet
        val fromNetwork = updateXsMap.keySet
        val newFeeds = (fromNetwork diff inDbUrls).flatMap(updateXsMap.get)
        newFeeds.map(insert)

        val newUrls = forceUpdateXs.map(_.url) ++ newFeeds.map(_.url)

        bySource(sourceId)
          .filter(_.url inSet newUrls)
          .result
      }
    }
  }

  private def getReadValues(unreadOnly: Boolean): Vector[Boolean] = {
    if (unreadOnly) {
      Vector(false)
    } else {
      Vector(true, false)
    }
  }

  private def commonPaging(tq: Query[driver.Feeds, Feed, Seq], offset: Int, limit: Int) = {
    tq
      .sortBy(_.publishedDate.desc)
      .drop(offset)
      .take(limit)
  }

  private def fetchPage(q: Query[driver.Feeds, Feed, Seq], offset: Int, limit: Int): FPage = {
    val act = for {
      resources <- q.drop(offset).take(limit).result
      total <- q.length.result
    } yield {
      (resources, total)
    }
    db.run(act)
  }

  private def ft[T](f: Future[T]): Task[T] = {
    Task.fromFuture { implicit ec => f }
  }

}
