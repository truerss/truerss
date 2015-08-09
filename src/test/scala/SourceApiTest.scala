

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
class SourceApiTest extends FunSpec with Matchers
  with ScalatestRouteTest with Routing with Common {

  import Gen._
  import truerss.models.ApiJsonProtocol._
  import truerss.util.Util._

  def actorRefFactory = system

  val dbRef = system.actorOf(Props(new DbActor(db, driver)), "test-db")
  val networkRef = TestProbe().ref
  val proxyRef = system.actorOf(Props(new ProxyActor(dbRef, networkRef)), "test-proxy")
  val context = system

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
      val name = genName(sources.map(_.name))
      val url = genUrl(sources.map(_.url))
      val normalized = name.normalize
      val source = genSource(None).copy(url = url, name = name,
        normalized = normalized)
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
        // !!!!
        ids = ids - last
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

  describe("Update source") {
    it ("update - 400 bad request when json not valid") {
      Put(s"${sourceUrl}/2", "{}") ~> computeRoute ~> check {
        responseAs[String] should be("Not valid json")
        status should be(StatusCodes.BadRequest)
      }
    }

    it ("update - 400 when url already present in db") {
      val last = ids.last
      val first = ids(0)
      val url = sources(first.toInt).url
      val json = sources(last.toInt).copy(url = url).toJson.toString
      Put(s"${sourceUrl}/${last}", json) ~> computeRoute ~> check {
        responseAs[String] should be("Url already present in db")
        status should be(StatusCodes.BadRequest)
      }
    }

    it ("400 when name already present in db") {
      val last = ids.last
      val first = ids(0)
      val name = sources(first.toInt).name
      val json = sources(last.toInt).copy(name = name).toJson.toString
      Put(s"${sourceUrl}/${last}", json) ~> computeRoute ~> check {
        responseAs[String] should be("Name not unique")
        status should be(StatusCodes.BadRequest)
      }
    }

    it ("400 when interval or url not valid") {
      val last = ids.last
      val json = sources(last.toInt).copy(interval = 0, url = "abc").toJson.toString
      Put(s"${sourceUrl}/${last}", json) ~> computeRoute ~> check {
        responseAs[String] should be("Interval must be great than 0, Not valid url")
        status should be(StatusCodes.BadRequest)
      }
    }

    it ("200 ok update source") {
      val last = ids.last
      val newUrl = genUrl(sources.map(_.url))
      val newName = genName(sources.map(_.name))
      val normalized = newName.normalize
      val source = sources(last.toInt).copy(id = Some(last), url = newUrl,
        name = newName, normalized = normalized)
      val json = source.toJson.toString

      Put(s"${sourceUrl}/${last}", json) ~> computeRoute ~> check {
        val resp = JsonParser(responseAs[String]).convertTo[Source]
        resp.id should be(source.id)
        resp.name should be(source.name)
        resp.url should be(source.url)
        resp.interval should be(source.interval)
        status should be(StatusCodes.OK)
        val updated = db withSession { implicit session =>
          driver.query.sources.filter(s => s.id === last).firstOption
        }
        updated.get.url should be(newUrl)
        updated.get.name should be(newName)
      }
    }
  }

  describe("Mark all") {
    it("404 when source not found") {
      Put(s"$sourceUrl/mark/1000") ~> computeRoute ~> check {
        responseAs[String] should be("Source not found")
        status should be(StatusCodes.NotFound)
      }
    }

    it("mark all as read for source") {
      val sourceId = feeds.filterNot(_.read)(0).sourceId
      Put(s"$sourceUrl/mark/${sourceId}") ~> computeRoute ~> check {
        val count = db withSession { implicit session =>
          driver.query.feeds
            .filter(f => f.sourceId === sourceId && f.read === false).length.run
        }
        count should be(0)
      }
    }
  }

  describe("Lates") {
    it("return all non read feeds") {
      Get(s"${sourceUrl}/latest/10") ~> computeRoute ~> check {
        val res = JsonParser(responseAs[String]).convertTo[Vector[Feed]]
        res.size should be > 0
      }
    }
  }

  describe("Extract feeds for source") {
    it("return all feeds for source") {
      val sourceId = ids(0)
      Get(s"${sourceUrl}/feeds/${sourceId}") ~> computeRoute ~> check {
        val result = JsonParser(responseAs[String]).convertTo[Vector[Feed]]
        result.forall(_.sourceId == sourceId) should be(true)
      }
    }
  }

}
