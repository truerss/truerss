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
    sources.result ~> db
  }

  def findOne(sourceId: Long): Task[Option[Source]] = {
    sources.filter(_.id === sourceId).take(1).result.headOption ~> db
  }

  def delete(sourceId: Long): Task[Int] = {
    sources.filter(_.id === sourceId).delete ~> db
  }

  def insert(source: Source): Task[Long] = {
    ((sources returning sources.map(_.id)) += source) ~> db
  }

  def insertMany(xs: Iterable[Source]) = {
    (sources ++= xs) ~> db
  }

  def findByUrls(urls: Seq[String]): Task[Seq[String]] = {
    sources.filter(s => s.url.inSet(urls))
      .map(_.url)
      .result ~> db
  }

  def fetchByUrls(urls: Seq[String]): Task[Seq[Source]] = {
    sources.filter(s => s.url.inSet(urls))
      .result ~> db
  }

  def findByNames(names: Seq[String]): Task[Seq[String]] = {
    sources.filter(s => s.name.inSet(names))
      .map(_.url)
      .result ~> db
  }

  def findByUrl(url: String, id: Option[Long]): Task[Int] = {
    id
      .map(id => sources.filter(s => s.url === url && !(s.id === id)))
      .getOrElse(sources.filter(s => s.url === url))
      .length
      .result ~> db
  }

  def findByName(name: String, id: Option[Long]): Task[Int] = {
    id.map(id => sources.filter(s => s.name === name && !(s.id === id)))
      .getOrElse(sources.filter(s => s.name === name))
      .length
      .result ~> db
  }

  def updateSource(source: Source): Task[Int] = {
    sources.filter(_.id === source.id)
      .map(s => (s.url, s.name, s.interval, s.state, s.normalized))
      .update(source.url, source.name, source.interval,
        source.state, source.normalized) ~> db
  }

  def updateLastUpdateDate(sourceId: Long,
                           date: LocalDateTime = LocalDateTime.now(Clock.systemUTC())): Task[Int] = {
    sources.filter(_.id === sourceId)
      .map(s => s.lastUpdate).update(date) ~> db
  }

  def updateState(sourceId: Long, state: SourceState): Task[Int] = {
    sources.filter(_.id === sourceId)
      .map(_.state)
      .update(state) ~> db
  }

}
