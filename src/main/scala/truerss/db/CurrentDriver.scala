package truerss.db

import java.util.Date

import slick.jdbc.JdbcProfile
import slick.sql.SqlProfile.ColumnOption.{NotNull, Nullable, SqlType}
import truerss.models._

/**
  * Created by mike on 6.5.17.
  */
case class CurrentDriver(profile: JdbcProfile) {

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
    lazy val feeds = TableQuery[Feeds]
  }

}
