package truerss.models

import java.util.Date

import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.util.Jsonize

import scala.language.postfixOps
import slick.jdbc.JdbcProfile
import slick.sql.SqlProfile.ColumnOption._
import truerss.util.Util._
import truerss.util.{Lens => L}

import scala.concurrent.{ExecutionContext, Future}

sealed trait SourceState
case object Neutral extends SourceState
case object Enable extends SourceState
case object Disable extends SourceState

case class Source(id: Option[Long],
                  url: String,
                  name: String,
                  interval: Int,
                  state: SourceState,
                  normalized: String,
                  lastUpdate: Date,
                  count: Int = 0) extends Jsonize {

  def normalize: Source = L.normalized.set(this)(name.normalize)
  def recount(x: Int): Source = L.count.set(this)(x)
  def newId(x: Long): Source = L.id.set(this)(Some(x))
  def withState(x: SourceState): Source = L.state.set(this)(x)

}

object SourceHelper {
  def from(url: String, name: String, interval: Int): Source = {
    Source(
      id = None,
      url = url,
      name = name,
      interval = interval,
      state = Neutral,
      normalized = name,
      lastUpdate = new Date()
    )
  }
}

case class Feed(id: Option[Long],
                sourceId: Long,
                url: String,
                title: String,
                author: String,
                publishedDate: Date,
                description: Option[String],
                content: Option[String],
                normalized: String,
                favorite: Boolean = false,
                read: Boolean = false,
                delete: Boolean = false) extends Jsonize {
  def mark(flag: Boolean): Feed = L.fav.set(this)(flag)
}

case class WSMessage(messageType: String, body: String)

case class CurrentDriver(profile: JdbcProfile) {

  import profile.api._

  object DateSupport {
    implicit val javaUtilDateMapper =
      MappedColumnType.base[Date, java.sql.Timestamp] (
        d => new java.sql.Timestamp(d.getTime),
        d => new java.util.Date(d.getTime))
  }

  object StateSupport {
    implicit val sourceStateMapper = MappedColumnType.base[SourceState, Byte](
      state => state match {
        case Neutral => 0
        case Enable => 1
        case Disable => 2
      },
      b => b match {
        case 0 => Neutral
        case 1 => Enable
        case 2 => Disable
      }
    )
  }

  class Sources(tag: Tag) extends Table[Source](tag, "sources") {
    import DateSupport._
    import StateSupport._


    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def url = column[String]("url")

    def name = column[String]("name")

    def interval = column[Int]("interval", O.Default[Int](86400))

    def state = column[SourceState]("state")

    def normalized = column[String]("normalized")

    def lastUpdate = column[Date]("lastupdate")

    def count = column[Int]("count", O.Default(0)) // ignored

    def * = (id.?, url, name, interval, state,
      normalized, lastUpdate, count) <> (Source.tupled, Source.unapply)

  }

  class Feeds(tag: Tag) extends Table[Feed](tag, "feeds") {
    import DateSupport._

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def sourceId = column[Long]("source_id", NotNull)

    def url = column[String]("url")

    def title = column[String]("title", SqlType("TEXT"))

    def author = column[String]("author")

    def publishedDate = column[Date]("published_date")

    def description = column[String]("description", Nullable, SqlType("TEXT"))

    def content = column[String]("content", Nullable, SqlType("TEXT"))

    def normalized = column[String]("normalized")

    def favorite = column[Boolean]("favorite", O.Default(false))

    def read = column[Boolean]("read", O.Default(false))

    def delete = column[Boolean]("delete", O.Default(false))

    def * = (id.?, sourceId, url, title, author, publishedDate, description.?, content.?, normalized, favorite, read, delete) <> (Feed.tupled, Feed.unapply)

    def source = foreignKey("source_fk", sourceId, query.sources)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
  }

  object query {
    lazy val sources = TableQuery[Sources]
    lazy val feeds   = TableQuery[Feeds]

    lazy val actualFeeds = feeds.filter(_.delete === false)
  }


}

class SourceDao(val db: DatabaseDef)(implicit
                                 val ec: ExecutionContext,
                                 driver: CurrentDriver
) {

  import driver.profile.api._
  import driver.query.sources
  import driver.DateSupport._
  import driver.StateSupport._

  def all: Future[Seq[Source]] = db.run(sources.result)

  def findOne(sourceId: Long): Future[Option[Source]] = {
    db.run(sources.filter(_.id === sourceId).take(1).result)
      .map(_.headOption)
  }

  def delete(sourceId: Long): Future[Int] = {
    db.run(sources.filter(_.id === sourceId).delete)
  }

  def insert(source: Source): Future[Long] = {
    db.run {
      (sources returning sources.map(_.id)) += source
    }
  }

  def findByUrl(url: String, id: Option[Long]): Future[Int] = {
    db.run {
      id
        .map(id => sources.filter(s => s.url === url && !(s.id === id)))
        .getOrElse(sources.filter(s => s.url === url))
        .length
        .result
    }
  }

  def findByName(name: String, id: Option[Long]): Future[Int] = {
    db.run {
      id.map(id => sources.filter(s => s.name === name && !(s.id === id)))
        .getOrElse(sources.filter(s => s.name === name))
        .length
        .result
    }
  }

  def updateSource(source: Source): Future[Int] = {
    db.run {
      sources.filter(_.id === source.id)
        .map(s => (s.url, s.name, s.interval, s.state, s.normalized))
        .update(source.url, source.name, source.interval,
          source.state, source.normalized)
    }
  }

  def updateLastUpdateDate(sourceId: Long): Future[Int] = {
    db.run {
      sources.filter(_.id === sourceId)
        .map(s => s.lastUpdate).update(new Date())
    }
  }

  def updateState(sourceId: Long, state: SourceState): Future[Int] = {
    db.run {
      sources.filter(_.id === sourceId)
        .map(_.state)
        .update(state)
    }
  }



}




















