
import org.scalatest.BeforeAndAfterAll
import truerss.db.{H2, DBProfile}
import truerss.models.{Feed, CurrentDriver}

import scala.slick.jdbc.JdbcBackend

trait Common extends BeforeAndAfterAll with DbHelper { self : org.scalatest.Suite =>

  import Gen._

  val sourceUrl = "/api/v1/sources"
  val feedUrl = "/api/v1/feeds"

  import driver.profile.simple._

  val source1 = genSource()
  val sources = Vector(source1, genSource(), genSource())
  var ids = scala.collection.mutable.ArrayBuffer[Long]()
  var feedIds = scala.collection.mutable.ArrayBuffer[Long]()
  var unfavAndUnReadId: Long = _
  var favAndReadId: Long = _
  var unfavAndUnRead: Feed = _
  var favAndRead: Feed = _
  var feeds = scala.collection.mutable.ArrayBuffer[Feed]()

  def getId(id: Long, max: Int = 2): Int = {
    val k = (id / max).toInt
    if (k > max) {
      getId(k, max)
    } else {
      if (k == 0) {
        k
      } else {
        k - 1
      }
    }
  }

  def create = {
    db withSession { implicit session =>
      (driver.query.sources.ddl ++ driver.query.feeds.ddl).create
      val z = sources.map { source =>
        (driver.query.sources returning driver.query.sources.map(_.id)) += source
      }
      ids = z.to[scala.collection.mutable.ArrayBuffer]
      feedIds = ids.map { id =>
        val url = sources(getId(id, sources.size - 1)).url
        (0 to 10).map { fId =>
          val f = genFeed(id, url)
          val fId = (driver.query.feeds returning driver.query.feeds.map(_.id)) += f
          feeds += f.copy(id = Some(fId))
          fId
        }
      }.flatten

      val sId = ids(0)
      val url = sources(sId.toInt).url
      val feed = genFeed(sId, url).copy(favorite = false, read = false)
      unfavAndUnReadId = (driver.query.feeds returning driver.query.feeds.map(_.id)) += feed
      unfavAndUnRead = feed.copy(id = Some(unfavAndUnReadId))

      val feed1 = genFeed(sId, url).copy(favorite = true, read = true)
      favAndReadId = (driver.query.feeds returning driver.query.feeds.map(_.id)) += feed1
      favAndRead = feed1.copy(id = Some(favAndReadId))
    }
  }

  def clean = {
    db withSession { implicit session =>
      (driver.query.sources.ddl ++ driver.query.feeds.ddl).drop
    }
  }

  override def beforeAll() = create
  override def afterAll() = clean

}
