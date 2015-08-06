
import io.codearte.jfairy.Fairy
import java.util.{Date, UUID}
import truerss.models
/**
 * Created by mike on 2.8.15.
 */
object Gen {
  import truerss.util.Util._
  import models.Source

  private val fairy = Fairy.create()

  def genId = UUID.randomUUID().toString


  def genSource(id: Option[Long] = None) = {
    val name = fairy.company().name()
    Source(id = id,
      url = fairy.company().url(),
      name = name,
      interval = fairy.baseProducer().randomBetween(1, 12),
      plugin = false,
      normalized = name.normalize,
      lastUpdate = new Date(),
      error = false
    )
  }


}
