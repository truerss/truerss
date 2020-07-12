package truerss.db

import java.time.{Clock, LocalDateTime}

import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.CurrentDriver
import zio.Task

import scala.concurrent.{ExecutionContext, Future}

class SourceDao(val db: DatabaseDef)(implicit
                                     val ec: ExecutionContext,
                                     driver: CurrentDriver
) {

  import driver.StateSupport._
  import driver.profile.api._
  import driver.query.sources

  def all: Task[Seq[Source]] = {
    ft(db.run(sources.result))
  }

  def findOne(sourceId: Long): Task[Option[Source]] = {
    ft {
      db.run(sources.filter(_.id === sourceId).take(1).result)
        .map(_.headOption)
    }
  }

  def delete(sourceId: Long): Task[Int] = {
    ft(db.run(sources.filter(_.id === sourceId).delete))
  }

  def insert(source: Source): Task[Long] = {
    ft {
      db.run {
        (sources returning sources.map(_.id)) += source
      }
    }
  }

  def insertMany(xs: Iterable[Source]) = {
    ft {
      db.run {
        sources ++= xs
      }
    }
  }

  def findByUrls(urls: Seq[String]): Task[Seq[String]] = {
    ft {
      db.run {
        sources.filter(s => s.url.inSet(urls))
          .map(_.url)
          .result
      }
    }
  }

  def fetchByUrls(urls: Seq[String]): Task[Seq[Source]] = {
    Task.fromFuture { implicit ec =>
      db.run {
        sources.filter(s => s.url.inSet(urls))
          .result
      }
    }
  }

  def findByNames(names: Seq[String]): Task[Seq[String]] = {
    Task.fromFuture { implicit ec =>
      db.run {
        sources.filter(s => s.name.inSet(names))
          .map(_.url)
          .result
      }
    }
  }

  def findByUrl(url: String, id: Option[Long]): Task[Int] = {
    val f = db.run {
      id
        .map(id => sources.filter(s => s.url === url && !(s.id === id)))
        .getOrElse(sources.filter(s => s.url === url))
        .length
        .result
    }
    Task.fromFuture {implicit ec => f }
  }

  def findByName(name: String, id: Option[Long]): Task[Int] = {
    val f = db.run {
      id.map(id => sources.filter(s => s.name === name && !(s.id === id)))
        .getOrElse(sources.filter(s => s.name === name))
        .length
        .result
    }
    Task.fromFuture { implicit ec => f }
  }

  def updateSource(source: Source): Task[Int] = {
    ft {
      db.run {
        sources.filter(_.id === source.id)
          .map(s => (s.url, s.name, s.interval, s.state, s.normalized))
          .update(source.url, source.name, source.interval,
            source.state, source.normalized)
      }
    }
  }

  def updateLastUpdateDate(sourceId: Long,
                           date: LocalDateTime = LocalDateTime.now(Clock.systemUTC())): Task[Int] = {
    ft {
      db.run {
        sources.filter(_.id === sourceId)
          .map(s => s.lastUpdate).update(date)
      }
    }
  }

  def updateState(sourceId: Long, state: SourceState): Task[Int] = {
    ft {
      db.run {
        sources.filter(_.id === sourceId)
          .map(_.state)
          .update(state)
      }
    }
  }

  private def ft[T](f: Future[T]): Task[T] = {
    Task.fromFuture { implicit ec => f }
  }

}
