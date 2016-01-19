
import java.util.{Date, UUID}

import io.codearte.jfairy.Fairy
import truerss.models
import truerss.models.Neutral
import java.time.LocalDateTime

object Gen {
  import models.{Feed, Source}
  import truerss.util.Util._

  private val fairy = Fairy.create()

  def genId = UUID.randomUUID().toString

  def genName(xs: Vector[String]): String = {
    val name: String = genName
    if (xs.contains(name))
      genName(xs)
    else
      name
  }

  def genUrl(xs: Vector[String]): String = {
    val url: String = genUrl
    if (xs.contains(url))
      genUrl(xs)
    else
      url
  }

  def genName = fairy.company().name()

  def genUrl = fairy.company().url()

  def genAuthor = fairy.person().fullName()

  def genText = fairy.textProducer().paragraph()

  def genSource(id: Option[Long] = None) = {
    val name = genName
    val z = LocalDateTime.now()

    Source(id = id,
      url = genUrl,
      name = name,
      interval = fairy.baseProducer().randomBetween(1, 12),
      state = Neutral,
      normalized = name.normalize,
      lastUpdate = z.plusDays(1).toDate
    )
  }

  def genFeed(sourceId: Long, sourceUrl: String) = {
    val title = genName
    Feed(
      id = None,
      sourceId = sourceId,
      url = s"${sourceUrl}/feed/${genId}",
      title = title,
      author = genAuthor,
      publishedDate = new Date(),
      description = Some(genText),
      content = None,
      normalized = title.normalize,
      favorite = fairy.baseProducer().trueOrFalse(),
      read = fairy.baseProducer().trueOrFalse(),
      delete = false
    )
  }


}
