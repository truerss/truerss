package truerss.util

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import com.github.truerss.base.Entry
import truerss.api.{ModelResponse, NotFoundResponse, OkResponse}
import truerss.models.{Enable, Feed, Neutral}


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

  trait ResponseHelpers {
    val ok = OkResponse("ok")
    val sourceNotFound = NotFoundResponse("Source not found")
    val feedNotFound = NotFoundResponse("Feed not found")
    def optionFeedResponse[T <: Jsonize](x: Option[T]) = x match {
      case Some(m) => ModelResponse(m)
      case None => feedNotFound
    }
  }

  object ResponseHelpers extends ResponseHelpers


}




