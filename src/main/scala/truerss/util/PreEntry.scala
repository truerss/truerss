package truerss.util

import java.util.Date

import com.github.truerss.base.Entry

case class PreEntry(
   url: Option[String],
   title: Option[String],
   author: Option[String],
   publishedDate: Date,
   description: Option[String]
) {

  import PreEntry.maxLength

  def toEntry: Entry = {
    val length = title.map(_.length).map{ length =>
      if (length > maxLength) {
        maxLength
      } else {
        length
      }
    }.getOrElse(0)

    Entry(
      url = url.get,
      title = title.map(x => x.substring(0, length)).getOrElse("no-title"),
      author = author.getOrElse(""),
      description = description,
      publishedDate = publishedDate,
      content = None
    )
  }
}

object PreEntry {
  val maxLength = 250
}
