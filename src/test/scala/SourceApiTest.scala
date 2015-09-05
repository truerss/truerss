

import akka.actor.Props
import akka.testkit.TestProbe
import org.scalatest._
import spray.http.StatusCodes
import spray.json.{JsonParser, _}
import spray.testkit.ScalatestRouteTest
import truerss.api._
import truerss.controllers.OkResponse
import truerss.db.DbActor
import truerss.models.{SourceForFrontend, Feed, Source}
import truerss.system.ProxyServiceActor
import truerss.system.util.NewSource
import truerss.util.ApplicationPlugins

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.slick.driver.H2Driver.simple._


class SourceApiTest extends FunSpec with Matchers
  with ScalatestRouteTest with Routing with Common {

  import Gen._
  import truerss.models.ApiJsonProtocol._
  import truerss.system.util.Update
  import truerss.util.Util._

  def actorRefFactory = system

  val dbRef = system.actorOf(Props(new DbActor(db, driver)), "test-db")
  val sourcesRef = TestProbe()
  val proxyRef = system.actorOf(Props(new ProxyServiceActor(
    ApplicationPlugins(),
    dbRef, sourcesRef.ref)), "test-proxy")
  val context = system

  val computeRoute = route(proxyRef, context, 8081)

  describe("GetAll") {
    it("should return all sources from db") {
      Get(s"${sourceUrl}/all") ~> computeRoute ~> check {
        JsonParser(responseAs[String])
          .convertTo[Vector[SourceForFrontend]].size should be(3)
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

      val request = Post(s"${sourceUrl}/create", json.toString) ~> computeRoute
      sourcesRef.expectMsgAnyClassOf(classOf[NewSource])

      request ~> check {
        val givenSource = JsonParser(responseAs[String])
          .convertTo[SourceForFrontend]

        givenSource.name should be(source.name)
        givenSource.url should be(source.url)
        givenSource.state should be(source.state)

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
      val source = sources.head
      val json = source.toJson.toString
      val url = source.url
      val name = source.name
      Post(s"${sourceUrl}/create", json) ~> computeRoute ~> check {
        responseAs[String] should be(s"Url '${url}' already present in db, Name '${name}' not unique")
        status should be(StatusCodes.BadRequest)
      }
    }

  }

  describe("Delete source") {
    it("delete source from db") {
      val last = ids.last
      Delete(s"${sourceUrl}/${last}") ~> computeRoute ~> check {
        responseAs[String] should be("ok")
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
      val url = source1.url
      val json = sources(last.toInt).copy(url = url).toJson.toString
      Put(s"${sourceUrl}/${last}", json) ~> computeRoute ~> check {
        responseAs[String] should be(s"Url '${url}' already present in db")
        status should be(StatusCodes.BadRequest)
      }
    }

    it ("400 when name already present in db") {
      val last = ids.last
      val name = source1.name
      val json = sources(last.toInt).copy(name = name).toJson.toString
      Put(s"${sourceUrl}/${last}", json) ~> computeRoute ~> check {
        responseAs[String] should be(s"Name '${name}' not unique")
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
        responseAs[String] should be("Source with id = 1000 not found")
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

  describe("Refresh") {
    it("refresh all source actors") {
      val resp = Put(s"${sourceUrl}/refresh") ~> computeRoute

      sourcesRef.expectMsg(1 seconds, Update)
      sourcesRef.reply(OkResponse("ok"))

      resp ~> check {
        responseAs[String] should be("ok")
        status should be(StatusCodes.OK)
      }
    }
  }



}
