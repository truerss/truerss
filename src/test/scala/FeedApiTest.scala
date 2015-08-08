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
class FeedApiTest extends FunSpec with BeforeAndAfterAll with Matchers
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
  var feeds: Vector[Feed] = _

  override def beforeAll() = {
    db withSession { implicit session =>
      (driver.query.sources.ddl ++ driver.query.feeds.ddl).create
      val z = sources.map { source =>
        (driver.query.sources returning driver.query.sources.map(_.id)) += source
      }
      ids = z.to[scala.collection.mutable.ArrayBuffer]
      feedIds = ids.map { id =>
        val url = sources((id - 1).toInt).url
        (0 to 10).map { fId =>
           val f = genFeed(id, url)
           (driver.query.feeds returning driver.query.feeds.map(_.id)) += f
        }
      }.flatten

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
        JsonParser(responseAs[String]).convertTo[Vector[Feed]]
          .forall(_.favorite) should be(true)
        status should be(StatusCodes.OK)
      }
    }
  }

  override def afterAll() = {
    db withSession { implicit session =>
      (driver.query.sources.ddl ++ driver.query.feeds.ddl).drop
    }
  }

}
