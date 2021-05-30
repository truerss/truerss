package truerss.util

import java.time.ZoneOffset
import com.github.truerss.base.{Enclosure, Entry}
import org.jsoup.Jsoup
import truerss.db.Feed
import truerss.plugins.EntryDto

object EntryImplicits {

  import CommonImplicits._

  private def enclosuretoStr(enc: Enclosure): String = {
    import enc._
    s"${`type`}|$url|$length"
  }

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
      enclosure = entry.enclosure.map(enclosuretoStr),
      normalized = entry.title.normalize,
      favorite = false,
      read = false,
      delete = false
    )
  }

  implicit class EntryDtoExt(val entry: EntryDto) extends AnyVal {
    def clearImages: EntryDto = {
      entry.description match {
        // remove images from description
        case Some(d) if d.contains("<img") =>
          entry.copy(description =  Some(Jsoup.parse(d).select("img").remove().text()))
        case _ => entry
      }
    }
  }

}
