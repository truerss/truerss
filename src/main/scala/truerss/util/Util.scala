package truerss.util

import java.time.{LocalDateTime, ZoneId, LocalDate}
import java.util.Date

import com.github.truerss.base.Entry
import truerss.models.Feed


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

}




