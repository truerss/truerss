package truerss.models

import java.util.Date
import truerss.util.Jsonize
import scala.language.postfixOps
import scala.slick.driver.JdbcProfile

import truerss.util.Util._

/**
 * Created by mike on 1.8.15.
 */
case class Source(id: Option[Long],
                  url: String,
                  name: String,
                  interval: Int,
                  plugin: Boolean,
                  normalized: String,
                  lastUpdate: Date,
                  error: Boolean = false) extends Jsonize {
  def normalize = copy(normalized = name.normalize)
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


case class FrontendSource(url: String, name: String, interval: Int) {
  def toSource = Source(
    id = None,
    url = url,
    name = name,
    interval = interval,
    plugin = false,
    normalized = name,
    lastUpdate = new Date(),
    error = false
  )
}

case class SourceForFrontend(
  id: Long,
  url: String,
  name: String,
  interval: Int,
  plugin: Boolean,
  normalized: String,
  lastUpdate: Date,
  count: Int = 0
) extends Jsonize



case class CurrentDriver(profile: JdbcProfile) {

  import profile.simple._

  object DateSupport {
    implicit val JavaUtilDateMapper =
      MappedColumnType .base[Date, java.sql.Timestamp] (
        d => new java.sql.Timestamp(d.getTime),
        d => new java.util.Date(d.getTime))
  }

  class Sources(tag: Tag) extends Table[Source](tag, "sources") {
    import DateSupport._

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def url = column[String]("url")

    def name = column[String]("name")

    def interval = column[Int]("interval", O.Default[Int](86400))

    def plugin = column[Boolean]("plugin", O.Default[Boolean](false))

    def normalized = column[String]("normalized")

    def lastUpdate = column[Date]("lastupdate")

    def error = column[Boolean]("error")

    def * = (id.?, url, name, interval, plugin, normalized, lastUpdate, error) <> (Source.tupled, Source.unapply)

  }

  class Feeds(tag: Tag) extends Table[Feed](tag, "feeds") {
    import DateSupport._

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def sourceId = column[Long]("source_id", O.NotNull)

    def url = column[String]("url")

    def title = column[String]("title")

    def author = column[String]("author")

    def publishedDate = column[Date]("published_date")

    def description = column[String]("description", O.Nullable, O.DBType("VARCHAR(6000)"))

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




















