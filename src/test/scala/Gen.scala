
import java.util.{Random, Date, UUID}

import truerss.models
import truerss.models.Neutral
import java.time.LocalDateTime

object Gen {
  import models.{Feed, Source}
  import truerss.util.Util._

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

  def genName = genId

  def genUrl = s"http://example${genId.replaceAll("-", "")}.com"

  def genAuthor = genId

  def genText = Vector.fill(10)(genId).mkString("\n")

  def genInt = new Random().nextInt(11) + 1

  def tOf = if (genInt / 2 == 0) true else false

  def genSource(id: Option[Long] = None) = {
    val name = genName
    val z = LocalDateTime.now()

    Source(id = id,
      url = genUrl,
      name = name,
      interval = genInt,
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
      url = s"$sourceUrl/feed/$genId",
      title = title,
      author = genAuthor,
      publishedDate = new Date(),
      description = Some(genText),
      content = None,
      normalized = title.normalize,
      favorite = tOf,
      read = tOf,
      delete = false
    )
  }


}
