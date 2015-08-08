

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
 * Created by mike on 2.8.15.
 */
class SourceApiTest extends FunSpec with BeforeAndAfterAll with Matchers
  with ScalatestRouteTest with Routing {

  import Gen._
  import truerss.models.ApiJsonProtocol._

  def actorRefFactory = system
  val dbProfile = DBProfile.create(H2)
  val db = JdbcBackend.Database.forURL("jdbc:h2:mem:test1;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = dbProfile.driver)
  val driver = new CurrentDriver(dbProfile.profile)
  import driver.profile.simple._

  db withSession { implicit session =>
    (driver.query.sources.ddl ++ driver.query.feeds.ddl).create
  }

  val sources = Vector(genSource(Some(1)), genSource(Some(2)), genSource(Some(3)))
  var ids = scala.collection.mutable.ArrayBuffer[Long]()

  override def beforeAll() = {
    db withSession { implicit session =>
      val z = sources.map { source =>
        (driver.query.sources returning driver.query.sources.map(_.id)) += source
      }
      ids = z.to[scala.collection.mutable.ArrayBuffer]
    }
  }


  val dbRef = system.actorOf(Props(new DbActor(db, driver)), "test-db")
  val proxyRef = system.actorOf(Props(new ProxyActor(dbRef)), "test-proxy")
  val context = system

  val sourceUrl = "/api/v1/sources"

  val computeRoute = route(proxyRef, context)

  describe("GetAll") {
    it("should return all sources from db") {
      Get(s"${sourceUrl}/all") ~> computeRoute ~> check {
        JsonParser(responseAs[String]).convertTo[Vector[Source]].size should be(3)
        status should be(StatusCodes.OK)
      }
    }
  }

  describe("Get source") {

    it("should return one source on request") {
      Get(s"${sourceUrl}/${ids.head}") ~> computeRoute ~> check {
        status should be(StatusCodes.OK)
      }
    }

    it("should return 404 when source not found") {
      Get(s"${sourceUrl}/1000") ~> computeRoute ~> check {
        responseAs[String] should be("Source with id = 1000 not found")
        status should be(StatusCodes.NotFound)
      }
    }
  }

  describe("Add source") {
    it("should create source on valid json") {
      val source = genSource(None)
      val json = source.toJson
      Post(s"${sourceUrl}/create", json.toString) ~> computeRoute ~> check {
        val givenSource = JsonParser(responseAs[String]).convertTo[Source]

        givenSource.name should be(source.name)
        givenSource.url should be(source.url)
        givenSource.plugin should be(source.plugin)

        status should be(StatusCodes.OK)
      }
    }

    it("bad request on not valid json") {
      Post(s"${sourceUrl}/create", "{}") ~> computeRoute ~> check {
        responseAs[String] should be("Not valid json")
        status should be(StatusCodes.BadRequest)
      }
    }

    it("bad request when url and interval not valid") {
      val json = genSource().copy(interval = -10, url = "abc").toJson.toString
      Post(s"${sourceUrl}/create", json) ~> computeRoute ~> check {
        responseAs[String] should be("Interval must be great than 0, Not valid url")
        status should be(StatusCodes.BadRequest)
      }
    }

    it("bad request when url and name not uniq") {
      val json = sources.head.toJson.toString
      Post(s"${sourceUrl}/create", json) ~> computeRoute ~> check {
        responseAs[String] should be("Url already present in db, Name not unique")
        status should be(StatusCodes.BadRequest)
      }
    }

  }

  describe("Delete source") {
    it("delete source from db") {
      val last = ids.last
      Delete(s"${sourceUrl}/${last}") ~> computeRoute ~> check {
        val source = JsonParser(responseAs[String]).convertTo[Source]
        source.id should be(Some(last))
        status should be(StatusCodes.OK)
        val extract = db withSession { implicit session =>
          driver.query.sources.buildColl
        }
        extract.map(_.id.get).contains(last) should be(false)
      }
    }

    it("404 when source not found") {
       Delete(s"${sourceUrl}/1000") ~> computeRoute ~> check {
         responseAs[String] should be("Source with id = 1000 not found")
         status should be(StatusCodes.NotFound)
       }
    }
  }


  override def afterAll() = {}



}
