package truerss.util

import truerss.models.Feed
import truerss.plugins.Entry
/**
 * Created by mike on 2.8.15.
 */
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

    def fromEntry(entry: Entry, sourceId: Long): Feed = Feed(
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


}

trait Jsonize

