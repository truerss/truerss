package truerss.util

import java.time.ZoneOffset

import com.github.truerss.base.Entry
import truerss.db.Feed

object EntryImplicits {

  import Util._

  implicit class EntryExt(val entry: Entry) extends AnyVal {
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

}
