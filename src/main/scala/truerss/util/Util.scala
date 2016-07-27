package truerss.util

import java.time.{LocalDateTime, ZoneId, LocalDate}
import java.util.Date

import com.github.truerss.base.Entry
import truerss.controllers.{ModelResponse, NotFoundResponse, OkResponse}
import truerss.models.{Neutral, Enable, Feed}
import truerss.system.db.Numerable


object Util {
  implicit class StringExt(s: String) {
    def normalize = s.replaceAll("[^\\p{L}\\p{Nd}]+", "-")
  }

  implicit class FeedExt(feed: Feed) {
    def toEntry: Entry = Entry(
      url = feed.url,
      title = feed.title,
      author = feed.author,
      publishedDate = feed.publishedDate,
      description = feed.description,
      content = feed.content
    )
  }

  implicit class EntryExt(entry: Entry) {
    def toFeed(sourceId: Long): Feed = Feed(
      id = None,
      sourceId = sourceId,
      url = entry.url,
      title = entry.title,
      publishedDate = entry.publishedDate,
      author = entry.author,
      description = entry.description,
      content = entry.content,
      normalized = entry.title.normalize,
      favorite = false,
      read = false,
      delete = false
    )
  }

  implicit class LocalDateExt(ld: LocalDateTime) {
    def toDate: Date = {
      Date.from(ld.atZone(ZoneId.systemDefault()).toInstant)
    }
  }

  // ?
  implicit class ApplicationPluginsExt(a: ApplicationPlugins) {
    def getState(url: String) = if (a.matchUrl(new java.net.URL(url))) {
      Enable
    } else {
      Neutral
    }
  }

  trait responseHelpers {
    val ok = OkResponse("ok")
    def sourceNotFound(x: Numerable) =
      NotFoundResponse(s"Source with id = ${x.num} not found")
    def sourceNotFound = NotFoundResponse(s"Source not found")
    def feedNotFound = NotFoundResponse(s"Feed not found")
    def feedNotFound(num: Long) = NotFoundResponse(s"Feed with id = ${num} not found")
    def optionFeedResponse[T <: Jsonize](x: Option[T]) = x match {
      case Some(m) => ModelResponse(m)
      case None => feedNotFound
    }
  }
  object responseHelpers extends responseHelpers


}




