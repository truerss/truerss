package truerss.models

import slick.jdbc.JdbcBackend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}

class FeedDao(val db: DatabaseDef)(implicit
                                   val ec: ExecutionContext,
                                   driver: CurrentDriver
) {

  import driver.DateSupport._
  import driver.profile.api._
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

}
