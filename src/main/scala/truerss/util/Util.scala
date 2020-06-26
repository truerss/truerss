package truerss.util

import java.time.{LocalDateTime, ZoneOffset}
import java.util.Date

import com.github.truerss.base.Entry
import truerss.db.{Feed, SourceState, SourceStates}

import scala.util.Try


object Util {
  implicit class StringExt(val s: String) extends AnyVal {
    def normalize: String = {
      s.replaceAll("[^\\p{L}\\p{Nd}]+", "-")
    }

    def toIntOr(recover: Int): Int = {
      Try(s.toInt).getOrElse(recover)
    }
  }

  implicit class EntryExt(entry: Entry) {
    def toFeed(sourceId: Long): Feed = Feed(
      id = None,
      sourceId = sourceId,
      url = entry.url,
      title = entry.title,
      publishedDate = entry.publishedDate.toInstant
        .atZone(ZoneOffset.UTC).toLocalDateTime,
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
      Date.from(ld.atZone(ZoneOffset.UTC).toInstant)
    }
  }

  // ?
  implicit class ApplicationPluginsExt(val a: ApplicationPlugins) extends AnyVal {
    def getState(url: String): SourceState = {
      if (a.matchUrl(url)) {
        SourceStates.Enable
      } else {
        SourceStates.Neutral
      }
    }
  }




}




