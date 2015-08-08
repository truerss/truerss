import akka.actor.Props
import akka.testkit.{TestProbe, TestActorRef}
import akka.util.Timeout
import akka.pattern.ask

import org.scalatest._
import spray.http.{StatusCodes, MediaTypes, HttpEntity}
import spray.httpx.unmarshalling.Unmarshaller
import truerss.system.ProxyActor
import truerss.db._
import truerss.models.{Source, Feed, CurrentDriver}

import scala.slick.driver.H2Driver.simple._
import scala.language.postfixOps
import scala.concurrent.duration._

import spray.routing._
import spray.testkit.RouteTest
import spray.json._
import spray.testkit.ScalatestRouteTest
import spray.json.JsonParser

import truerss.models._
import truerss.db.DbActor
import truerss.api._

import scala.slick.jdbc.JdbcBackend

/**
 * Created by mike on 8.8.15.
 */
class FeedApiTest extends FunSpec with BeforeAndAfterAll with BeforeAndAfter with Matchers
with ScalatestRouteTest with Routing  {

  import Gen._
  import truerss.models.ApiJsonProtocol._
  import truerss.util.Util._

  def actorRefFactory = system
  val dbProfile = DBProfile.create(H2)
  val db = JdbcBackend.Database.forURL("jdbc:h2:mem:test1;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = dbProfile.driver)
  val driver = new CurrentDriver(dbProfile.profile)
  import driver.profile.simple._

  val sources = Vector(genSource(Some(1)), genSource(Some(2)), genSource(Some(3)))
  var ids = scala.collection.mutable.ArrayBuffer[Long]()
  var feedIds = scala.collection.mutable.ArrayBuffer[Long]()
  var feeds = scala.collection.mutable.ArrayBuffer[Feed]()

  override def beforeAll() = {
    db withSession { implicit session =>
      (driver.query.sources.ddl ++ driver.query.feeds.ddl).create
    }
  }

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

  before {
    db withSession { implicit session =>
      ids = scala.collection.mutable.ArrayBuffer[Long]()
      feedIds = scala.collection.mutable.ArrayBuffer[Long]()
      feeds = scala.collection.mutable.ArrayBuffer[Feed]()
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
    }
  }

  after {
    db withSession { implicit session =>
      driver.query.feeds.delete
      driver.query.sources.delete
    }
  }


  val dbRef = system.actorOf(Props(new DbActor(db, driver)), "test-db")
  val proxyRef = system.actorOf(Props(new ProxyActor(dbRef)), "test-proxy")
  val context = system

  val computeRoute = route(proxyRef, context)

  val feedUrl = "/api/v1/feeds"

  describe("Favorites") {
    it("return all favorites feeds") {
      Get(s"${feedUrl}/favorites") ~> computeRoute ~> check {
        JsonParser(responseAs[String]).convertTo[Vector[Feed]].size should be(
          feeds.filter(_.favorite).size)
        status should be(StatusCodes.OK)
      }
    }
  }

  describe("Show") {
    it("return feed by id") {
      val id = feedIds(0)
      Get(s"${feedUrl}/${id}") ~> computeRoute ~> check {
        val original = feeds.filter(_.id == Some(id)).head
        val resp = JsonParser(responseAs[String]).convertTo[Feed]

        original.id should be(resp.id)
        original.title should be(resp.title)
        original.url should be(resp.url)
        original.description should be(resp.description)

        status should be(StatusCodes.OK)
      }
    }

    it("return 404 when id not found") {
      Get(s"${feedUrl}/1000") ~> computeRoute ~> check {
        responseAs[String] should be("Feed with id = 1000 not found")
        status should be(StatusCodes.NotFound)
      }
    }
  }

  describe("Mark as favorites") {
    it("404 when not found") {
      Put(s"${feedUrl}/mark/1000") ~> computeRoute ~> check {
        responseAs[String] should be("Feed not found")
        status should be(StatusCodes.NotFound)
      }
    }

    it("mark feed as favorite") {
      val feed = feeds.filterNot(_.favorite).head
      val firstNonFavorite = feed.id.get
      Put(s"${feedUrl}/mark/${firstNonFavorite}") ~> computeRoute ~> check {
        val resp = JsonParser(responseAs[String]).convertTo[Feed]
        feed.title should be(resp.title)
        feed.url should be(resp.url)
        status should be(StatusCodes.OK)

        val extract = db withSession { implicit session =>
          driver.query.feeds.filter(_.id === firstNonFavorite).firstOption
        }
        extract.get.favorite should be(true)
      }
    }
  }

  describe("Unmark as favorites") {
    it("404 when not found") {
      Put(s"${feedUrl}/unmark/1000") ~> computeRoute ~> check {
        responseAs[String] should be("Feed not found")
        status should be(StatusCodes.NotFound)
      }
    }

    it("unmark feed") {
      val feed = feeds.filter(_.favorite).reverse.head
      val firstFavorite = feed.id.get
      Put(s"${feedUrl}/unmark/${firstFavorite}") ~> computeRoute ~> check {
        val resp = JsonParser(responseAs[String]).convertTo[Feed]
        feed.title should be(resp.title)
        feed.url should be(resp.url)
        status should be(StatusCodes.OK)

        val extract = db withSession { implicit session =>
          driver.query.feeds.filter(_.id === firstFavorite).firstOption
        }
        extract.get.favorite should be(false)
      }
    }
  }

  describe("Mark as read") {
    it("404 when not found") {
      Put(s"${feedUrl}/read/1000") ~> computeRoute ~> check {
        responseAs[String] should be("Feed not found")
        status should be(StatusCodes.NotFound)
      }
    }
    it("mark feed as read") {
      val feed = feeds.filterNot(_.read).reverse.head
      val first = feed.id.get
      Put(s"${feedUrl}/read/${first}") ~> computeRoute ~> check {
        val resp = JsonParser(responseAs[String]).convertTo[Feed]
        feed.title should be(resp.title)
        feed.url should be(resp.url)
        status should be(StatusCodes.OK)

        val extract = db withSession { implicit session =>
          driver.query.feeds.filter(_.id === first).firstOption
        }
        extract.get.favorite should be(true)
      }
    }
  }

  describe("Mark as unread") {
    it("404 when not found") {
      Put(s"${feedUrl}/unread/1000") ~> computeRoute ~> check {
        responseAs[String] should be("Feed not found")
        status should be(StatusCodes.NotFound)
      }
    }
    it("mark feed as unread") {
      val feed = feeds.filter(_.read).head
      val first = feed.id.get
      Put(s"${feedUrl}/unread/${first}") ~> computeRoute ~> check {
        val resp = JsonParser(responseAs[String]).convertTo[Feed]
        feed.title should be(resp.title)
        feed.url should be(resp.url)
        status should be(StatusCodes.OK)

        val extract = db withSession { implicit session =>
          driver.query.feeds.filter(_.id === first).firstOption
        }
        extract.get.favorite should be(false)
      }
    }
  }



  override def afterAll() = {
    db withSession { implicit session =>
      (driver.query.sources.ddl ++ driver.query.feeds.ddl).drop
    }
  }

}
