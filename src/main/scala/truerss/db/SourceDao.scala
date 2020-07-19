package truerss.db

import java.time.{Clock, LocalDateTime}

import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.CurrentDriver
import zio.Task

class SourceDao(val db: DatabaseDef)(implicit
                                     driver: CurrentDriver
) {

  import JdbcTaskSupport._
  import driver.StateSupport._
  import driver.profile.api._
  import driver.query.sources

  def all: Task[Seq[Source]] = {
    db.go(sources.result)
  }

  def findOne(sourceId: Long): Task[Option[Source]] = {
    db.go(sources.filter(_.id === sourceId).take(1).result)
      .map(_.headOption)
  }

  def delete(sourceId: Long): Task[Int] = {
    db.go(sources.filter(_.id === sourceId).delete)
  }

  def insert(source: Source): Task[Long] = {
    db.go {
      (sources returning sources.map(_.id)) += source
    }
  }

  def insertMany(xs: Iterable[Source]) = {
    db.go { sources ++= xs }
  }

  def findByUrls(urls: Seq[String]): Task[Seq[String]] = {
    db.go {
      sources.filter(s => s.url.inSet(urls))
        .map(_.url)
        .result
    }
  }

  def fetchByUrls(urls: Seq[String]): Task[Seq[Source]] = {
    db.go {
      sources.filter(s => s.url.inSet(urls))
        .result
    }
  }

  def findByNames(names: Seq[String]): Task[Seq[String]] = {
    db.go {
      sources.filter(s => s.name.inSet(names))
        .map(_.url)
        .result
    }
  }

  def findByUrl(url: String, id: Option[Long]): Task[Int] = {
    db.go {
      id
        .map(id => sources.filter(s => s.url === url && !(s.id === id)))
        .getOrElse(sources.filter(s => s.url === url))
        .length
        .result
    }
  }

  def findByName(name: String, id: Option[Long]): Task[Int] = {
    db.go {
      id.map(id => sources.filter(s => s.name === name && !(s.id === id)))
        .getOrElse(sources.filter(s => s.name === name))
        .length
        .result
    }
  }

  def updateSource(source: Source): Task[Int] = {
    db.go {
      sources.filter(_.id === source.id)
        .map(s => (s.url, s.name, s.interval, s.state, s.normalized))
        .update(source.url, source.name, source.interval,
          source.state, source.normalized)
    }
  }

  def updateLastUpdateDate(sourceId: Long,
                           date: LocalDateTime = LocalDateTime.now(Clock.systemUTC())): Task[Int] = {
    db.go {
      sources.filter(_.id === sourceId)
        .map(s => s.lastUpdate).update(date)
    }
  }

  def updateState(sourceId: Long, state: SourceState): Task[Int] = {
    db.go {
      sources.filter(_.id === sourceId)
        .map(_.state)
        .update(state)
    }
  }

}
