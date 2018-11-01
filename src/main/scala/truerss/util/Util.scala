package truerss.util

import java.time.{LocalDateTime, ZoneId}
import java.util.Date

import com.github.truerss.base.Entry
import truerss.api.{NotFoundResponse, Ok}
import truerss.db.{Feed, SourceStates}


object Util {
  implicit class StringExt(s: String) {
    def normalize = s.replaceAll("[^\\p{L}\\p{Nd}]+", "-")
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
      SourceStates.Enable
    } else {
      SourceStates.Neutral
    }
  }

  object ResponseHelpers {
    val ok = Ok("ok")
    val sourceNotFound = NotFoundResponse("Source not found")
    val feedNotFound = NotFoundResponse("Feed not found")
  }


}




