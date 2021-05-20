package truerss.db.driver

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.Date

import slick.jdbc.{JdbcProfile, JdbcType}
import slick.sql.SqlProfile.ColumnOption.{NotNull, Nullable, SqlType}
import play.api.libs.json._
import slick.ast.BaseTypedType
import truerss.db._

/**
  * Created by mike on 6.5.17.
  */
case class CurrentDriver(profile: JdbcProfile, tableNames: TableNames) {

  import profile.api._

  object DateSupport {
    implicit val javaTimeMapper: JdbcType[LocalDateTime] =
      MappedColumnType.base[LocalDateTime, Long](
        d => d.withNano(0).toInstant(ZoneOffset.UTC).getEpochSecond,
        d =>
          LocalDateTime.ofInstant(Instant.ofEpochSecond(d), ZoneOffset.UTC)
            .withNano(0)
      )
  }

  object StateSupport {
    implicit val sourceStateMapper: JdbcType[SourceState] with BaseTypedType[SourceState] = MappedColumnType.base[SourceState, Byte](
      {
        case SourceStates.Neutral => 0
        case SourceStates.Enable => 1
        case SourceStates.Disable => 2
      },
      {
        case 0 => SourceStates.Neutral
        case 1 => SourceStates.Enable
        case 2 => SourceStates.Disable
      }
    )
  }

  object SettingValueSupport {

    implicit val inputValueMapper: JdbcType[SettingValue] with BaseTypedType[SettingValue] = MappedColumnType.base[SettingValue, String](
      value => Json.stringify(DbSettingJsonFormats.settingValueFormat.writes(value)),
      from => {
        DbSettingJsonFormats.settingValueFormat.reads(Json.parse(from))
          .getOrElse(SelectableValue.empty)
      }
    )
  }

  class Sources(tag: Tag) extends Table[Source](tag, tableNames.sources) {

    import StateSupport._

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def url = column[String]("url", O.Length(CurrentDriver.defaultLength))

    def name = column[String]("name", O.Length(CurrentDriver.defaultLength))

    def interval = column[Int]("interval", O.Default[Int](86400))

    def state = column[SourceState]("state")

    def normalized = column[String]("normalized")

    def lastUpdate = column[LocalDateTime]("lastupdate")

    def count = column[Int]("count", O.Default(0)) // ignored

    def byNameIndex = index("idx_name", name)

    def byUrlIndex = index("idx_url", url)

    def * = (id.?, url, name, interval, state,
      normalized, lastUpdate, count) <> (Source.tupled, Source.unapply)

  }

  class Feeds(tag: Tag) extends Table[Feed](tag, tableNames.feeds) {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def sourceId = column[Long]("source_id", NotNull)

    def url = column[String]("url")

    def title = column[String]("title", SqlType("TEXT"))

    def author = column[String]("author")

    def publishedDate = column[LocalDateTime]("published_date")

    def description = column[String]("description", Nullable, SqlType("TEXT"))

    def content = column[String]("content", Nullable, SqlType("TEXT"))

    def normalized = column[String]("normalized")

    def favorite = column[Boolean]("favorite", O.Default(false))

    def read = column[Boolean]("read", O.Default(false))

    def delete = column[Boolean]("delete", O.Default(false))

    def bySourceIndex = index("idx_source", sourceId)

    def byReadIndex = index("idx_read", read)

    def bySourceAndReadIndex = index("idx_source_read", (sourceId, read))

    def byFavoriteIndex = index("idx_favorites", favorite)

    def bySourceAndFavorite = index("idx_source_favorite", (sourceId, favorite))

    def * = (id.?, sourceId, url, title, author, publishedDate, description.?, content.?, normalized, favorite, read, delete) <> (Feed.tupled, Feed.unapply)

    def source = foreignKey("source_fk", sourceId, query.sources)(_.id, onUpdate = ForeignKeyAction.Restrict, onDelete = ForeignKeyAction.Cascade)
  }

  class Versions(tag: Tag) extends Table[Version](tag, tableNames.versions) {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def fact = column[String]("fact", SqlType("TEXT"))
    def when = column[LocalDateTime]("when", O.Default(LocalDateTime.now()))

    override def * = (id, fact, when) <> (Version.tupled, Version.unapply)
  }

  class PredefinedSettingsTable(tag: Tag) extends Table[PredefinedSettings](tag, tableNames.predefinedSettings) {
    import SettingValueSupport._

    def key = column[String]("key", O.Unique, O.Length(25))
    def description = column[String]("description")
    def value = column[SettingValue]("value")

    def byKeyIndex = index("idx_key", key)

    override def * = (key, description, value) <> (PredefinedSettings.tupled, PredefinedSettings.unapply)
  }

  class UserSettingsTable(tag: Tag) extends Table[UserSettings](tag, tableNames.userSettings) {
    def key = column[String]("key", O.Unique, O.PrimaryKey)
    def description = column[String]("description")
    def valueInt = column[Option[Int]]("valueInt")
    def valueBoolean = column[Option[Boolean]]("valueBoolean")
    def valueString = column[Option[String]]("valueString")

    override def * = (key, description, valueInt, valueBoolean, valueString) <> (UserSettings.tupled, UserSettings.unapply)
  }

  class PluginSourcesTable(tag: Tag) extends Table[PluginSource](tag, tableNames.pluginSources) {
    def id = column[Long]("id", O.PrimaryKey)
    def url = column[String]("url")

    override def * = (id.?, url) <> (PluginSource.tupled, PluginSource.unapply)
  }

  class SourceStatusesTable(tag: Tag) extends Table[SourceStatus](tag, tableNames.sourceStatuses) {
    def sourceId = column[Long]("sourceId", O.Unique)
    def errorCount = column[Int]("errorCount")

    override def * = (sourceId, errorCount) <> (SourceStatus.tupled, SourceStatus.unapply)
  }

  object query {
    lazy val sources = TableQuery[Sources]
    lazy val feeds = TableQuery[Feeds]
    def bySource(sourceId: Long): Query[Feeds, Feed, Seq] = {
      TableQuery[Feeds].filter(_.sourceId === sourceId)
    }
    def byFeed(feedId: Long): Query[Feeds, Feed, Seq] = {
      TableQuery[Feeds].filter(_.id === feedId)
    }

    lazy val versions = TableQuery[Versions]
    lazy val predefinedSettings = TableQuery[PredefinedSettingsTable]
    lazy val userSettings = TableQuery[UserSettingsTable]
    lazy val pluginSources = TableQuery[PluginSourcesTable]
    lazy val sourceStatuses = TableQuery[SourceStatusesTable]

    implicit class FeedsTQExt(val x: TableQuery[Feeds]) {
      def unreadOnly: Query[Feeds, Feed, Seq] = {
        x.filter(_.read === false)
      }

      def isFavorite: Query[Feeds, Feed, Seq] = {
        x.filter(_.favorite === true)
      }
    }

    implicit class FeedsQExt(val x: Query[Feeds, Feed, Seq]) {
      def unreadOnly: Query[Feeds, Feed, Seq] = {
        x.filter(_.read === false)
      }
      def isFavorite: Query[Feeds, Feed, Seq] = {
        x.filter(_.favorite === true)
      }
    }
  }

}

object CurrentDriver {
  final val defaultLength = 256
}

case class TableNames(sources: String,
                      feeds: String,
                      versions: String,
                      predefinedSettings: String,
                      userSettings: String,
                      pluginSources: String,
                      sourceStatuses: String,
                     )
object TableNames {
  val default = TableNames(
    sources = "sources",
    feeds = "feeds",
    versions = "versions",
    predefinedSettings = "predefined_settings",
    userSettings = "user_settings",
    pluginSources = "plugin_sources",
    sourceStatuses = "source_statuses"
  )

  def withPrefix(prefix: String): TableNames = {
    TableNames(
      sources = s"${prefix}_sources",
      feeds = s"${prefix}_feeds",
      versions = s"${prefix}_versions",
      predefinedSettings = s"${prefix}_predefined_settings",
      userSettings = s"${prefix}_user_settings",
      pluginSources = s"${prefix}_plugin_sources",
      sourceStatuses = s"${prefix}_source_statuses"
    )
  }
}