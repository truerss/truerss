
import io.codearte.jfairy.Fairy
import java.util.{Date, UUID}
import truerss.models
/**
 * Created by mike on 2.8.15.
 */
object Gen {
  import truerss.util.Util._
  import models.{Source, Feed}

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
    Source(id = id,
      url = genUrl,
      name = name,
      interval = fairy.baseProducer().randomBetween(1, 12),
      plugin = false,
      normalized = name.normalize,
      lastUpdate = new Date(),
      error = false
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
      read = false,
      delete = false
    )
  }


}
