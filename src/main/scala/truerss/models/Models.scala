package truerss.models

import java.util.Date
import truerss.util.Jsonize
import scala.language.postfixOps
import scala.slick.driver.JdbcProfile

import truerss.util.Util._
import truerss.util.{Lens => L}


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
                delete: Boolean = false) extends Jsonize

case class WSMessage(messageType: String, body: String)

case class CurrentDriver(profile: JdbcProfile) {

  import profile.simple._

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

    def sourceId = column[Long]("source_id", O.NotNull)

    def url = column[String]("url")

    def title = column[String]("title")

    def author = column[String]("author")

    def publishedDate = column[Date]("published_date")

    def description = column[String]("description", O.Nullable, O.DBType("TEXT"))

    def content = column[String]("content", O.Nullable, O.DBType("TEXT"))

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
    lazy val feedsWithEmptyContent = actualFeeds.map(f => (f.id, f.sourceId, f.url,
      f.title, f.author, f.publishedDate, f.description, f.favorite, f.read))
  }


}




















