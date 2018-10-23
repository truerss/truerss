package truerss.db.driver

import java.util.Date

import slick.jdbc.JdbcProfile
import slick.sql.SqlProfile.ColumnOption.{NotNull, Nullable, SqlType}
import truerss.db._

/**
  * Created by mike on 6.5.17.
  */
case class CurrentDriver(profile: JdbcProfile, tableNames: TableNames) {

  import profile.api._

  object DateSupport {
    implicit val javaUtilDateMapper =
      MappedColumnType.base[Date, java.sql.Timestamp](
        d => new java.sql.Timestamp(d.getTime),
        d => new java.util.Date(d.getTime))
  }

  object StateSupport {
    implicit val sourceStateMapper = MappedColumnType.base[SourceState, Byte](
      state => state match {
        case SourceStates.Neutral => 0
        case SourceStates.Enable => 1
        case SourceStates.Disable => 2
      },
      b => b match {
        case 0 => SourceStates.Neutral
        case 1 => SourceStates.Enable
        case 2 => SourceStates.Disable
      }
    )
  }

  class Sources(tag: Tag) extends Table[Source](tag, tableNames.sources) {

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

    def byNameIndex = index("idx_name", name)

    def byUrlIndex = index("idx_url", url)

    def * = (id.?, url, name, interval, state,
      normalized, lastUpdate, count) <> (Source.tupled, Source.unapply)

  }

  class Feeds(tag: Tag) extends Table[Feed](tag, tableNames.feeds) {

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

    def bySourceIndex = index("idx_source", sourceId)

    def byTitleIndex = index("idx_title", title)

    def byReadIndex = index("idx_read", read)

    def bySourceAndReadIndex = index("idx_source_read", (sourceId, read))

    def byFavoriteIndex = index("idx_favorites", favorite)

    def bySourceAndFavorite = index("idx_source_favorite", (sourceId, favorite))

    def * = (id.?, sourceId, url, title, author, publishedDate, description.?, content.?, normalized, favorite, read, delete) <> (Feed.tupled, Feed.unapply)

    def source = foreignKey("source_fk", sourceId, query.sources)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
  }

  class Versions(tag: Tag) extends Table[Version](tag, tableNames.versions) {
    import DateSupport._

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def fact = column[String]("fact", SqlType("TEXT"))
    def when = column[Date]("when", O.Default(new Date))

    override def * = (id, fact, when) <> (Version.tupled, Version.unapply)
  }

  object query {
    lazy val sources = TableQuery[Sources]
    lazy val feeds = TableQuery[Feeds]
    lazy val versions = TableQuery[Versions]
  }

}

case class TableNames(sources: String, feeds: String, versions: String)
object TableNames {
  val default = TableNames(
    sources = "sources",
    feeds = "feeds",
    versions = "versions"
  )

  def withPrefix(prefix: String): TableNames = {
    TableNames(
      sources = s"${prefix}_sources",
      feeds = s"${prefix}_feeds",
      versions = s"${prefix}_versions"
    )
  }
}