package truerss.plugins

import com.github.truerss.base.Entry
import truerss.dto.EnclosureDto
import truerss.util.EnclosureImplicits.EnclosureDtoExt

import java.util.Date

case class EntryDto(
   url: Option[String],
   title: Option[String],
   author: Option[String],
   publishedDate: Date,
   description: Option[String],
   enclosure: Option[EnclosureDto]
) {

  import EntryDto.maxLength

  def toEntry: Entry = {
    val length = title.map(t => Math.min(t.length, maxLength)).getOrElse(0)

    Entry(
      url = url.get,
      title = title.map(_.take(length)).getOrElse("no-title"),
      author = author.getOrElse(""),
      description = description,
      publishedDate = publishedDate,
      content = None,
      enclosure = enclosure.flatMap(_.toEnclosure)
    )
  }
}

object EntryDto {
  val maxLength = 250
}
