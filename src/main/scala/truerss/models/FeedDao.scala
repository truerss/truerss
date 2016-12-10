package truerss.models

import com.github.truerss.base.Entry
import slick.jdbc.JdbcBackend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}

class FeedDao(val db: DatabaseDef)(implicit
                                   val ec: ExecutionContext,
                                   driver: CurrentDriver
) {

  import driver.DateSupport._
  import driver.profile.api._
  import truerss.util.Util._
  import driver.query.feeds

  def findUnread(sourceId: Long): Future[Seq[Feed]] = {
    db.run {
      feeds.filter(_.sourceId === sourceId)
        .filter(_.read === false)
        .sortBy(_.publishedDate.desc)
        .result
    }
  }

  def feedBySourceCount(read: Boolean): Future[Seq[(Long, Int)]] = {
    db.run {
      feeds
        .filter(_.read === read)
        .groupBy(_.sourceId)
        .map {
          case (sourceId, xs) => sourceId -> xs.size
        }.result
    }
  }

  def feedCountBySourceId(sourceId: Long): Future[Int] = {
    db.run {
      feeds
        .filter(_.sourceId === sourceId)
        .length.result
    }
  }

  def deleteFeedsBySource(sourceId: Long): Future[Int] = {
    db.run {
      feeds.filter(_.sourceId === sourceId).delete
    }
  }

  def markAll: Future[Int] = {
    db.run {
      feeds
        .filter(_.read === false)
        .map(_.read)
        .update(true)
    }
  }

  def markBySource(sourceId: Long): Future[Int] = {
    db.run {
      feeds.filter(_.sourceId === sourceId)
        .map(f => f.read).update(true)
    }
  }

  def lastN(count: Long): Future[Seq[Feed]] = {
    db.run {
      feeds
        .filter(_.read === false)
        .take(count)
        .sortBy(_.publishedDate.desc)
        .result
    }
  }

  def pageForSource(sourceId: Long, from: Int, limit: Int): Future[Seq[Feed]] = {
    db.run {
      feeds
        .filter(_.sourceId === sourceId)
        .sortBy(_.publishedDate.desc)
        .drop(from)
        .take(limit)
        .result
    }
  }

  def favorites: Future[Seq[Feed]] = {
    db.run {
      feeds
        .filter(_.favorite === true)
        .result
    }
  }

  def findOne(feedId: Long): Future[Option[Feed]] = {
    db.run {
      feeds.filter(_.id === feedId).take(1).result
    }.map(_.headOption)
  }

  def modifyFav(feedId: Long, fav: Boolean) = {
    db.run {
      feeds.filter(_.id === feedId).map(e => e.favorite).update(fav)
    }
  }

  def modifyRead(feedId: Long, read: Boolean) = {
    db.run {
      feeds.filter(_.id === feedId).map(e => e.read).update(read)
    }
  }

  def updateContent(feedId: Long, content: String): Future[Int] = {
    db.run {
      feeds.filter(_.id === feedId).map(_.content).update(content)
    }
  }

  def mergeFeeds(sourceId: Long, xs: Vector[Entry]): Future[Seq[Feed]] = {
    val urls = xs.map(_.url)
    val q = feeds.filter(_.sourceId === sourceId)
      .filter(_.url inSet urls)
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
              q.filter(_.url === feed.url)
                .map(f => (f.title, f.author, f.publishedDate,
                  f.description, f.normalized, f.content, f.read))
                .update((feed.title, feed.author, feed.publishedDate,
                  feed.description.orNull,
                  feed.normalized, feed.content.orNull, false))
            case None =>
              feeds += feed
          }
        }

        val updateXsMap = updateXs.map(_.toFeed(sourceId)).map(f => f.url -> f).toMap
        val inDbUrls = inDbMap.keySet
        val fromNetwork = updateXsMap.keySet
        val newFeeds = (fromNetwork diff inDbUrls).flatMap(updateXsMap.get)
        feeds ++= newFeeds.toSeq

        val newUrls = forceUpdateXs.map(_.url) ++ newFeeds.map(_.url)
        feeds.filter(_.sourceId === sourceId)
          .filter(_.url inSet newUrls)
          .result
      }
    }
  }

}
