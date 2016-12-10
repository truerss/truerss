import java.util.concurrent.Executors

import akka.actor.Props
import akka.testkit.{TestProbe, TestActorRef}
import akka.util.Timeout
import akka.pattern.ask

import org.scalatest._
import spray.http.{StatusCodes, MediaTypes, HttpEntity}
import spray.httpx.unmarshalling.Unmarshaller
import truerss.system.ProxyServiceActor
import truerss.db._
import truerss.models.{Source, Feed, CurrentDriver}
import truerss.system.network.{ExtractError, ExtractContent, ExtractContentForEntry}

import scala.concurrent.ExecutionContext
import slick.jdbc.H2Profile.api._
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
import truerss.util.ApplicationPlugins

import slick.jdbc.JdbcBackend
/*
class FeedApiTest extends FunSpec with Matchers
with ScalatestRouteTest with Routing with Common {

  import truerss.models.ApiJsonProtocol._
  import truerss.util.Util._
  import truerss.system.util.PublishEvent

  def actorRefFactory = system

  val dbRef = system.actorOf(Props(new DbActor(db, driver)), "test-db")
  val publishActor = TestProbe()
  val sysActor = TestProbe()
  system.eventStream.subscribe(publishActor.ref, classOf[PublishEvent])
  val sourcesRef = TestProbe()
  val proxyRef = system.actorOf(Props(new ProxyServiceActor(
    ApplicationPlugins(),
    dbRef, sourcesRef.ref, sysActor.ref)), "test-proxy")
  val context = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  val computeRoute = route(proxyRef, context, 8080, Vector.empty, Vector.empty)

  describe("Favorites") {
    it("return all favorites feeds") {
      Get(s"${feedUrl}/favorites") ~> computeRoute ~> check {
        JsonParser(responseAs[String]).convertTo[Vector[Feed]].size should be(
          feeds.count(_.favorite) + 1)
        status should be(StatusCodes.OK)
      }
    }
  }

  describe("Show") {
    it("return feed by id") {
      val id = feedIds(0)
      val original = feeds.filter(_.id == Some(id)).head

      val req = Get(s"${feedUrl}/${id}") ~> computeRoute
      val content = Some("content")

      sourcesRef.expectMsg(1 seconds, ExtractContent(original.sourceId,
        id, original.url))
      sourcesRef.reply(ExtractContentForEntry(
        original.sourceId, id, content))

      req ~> check {
        val resp = JsonParser(responseAs[String]).convertTo[Feed]
        original.id should be(resp.id)
        original.title should be(resp.title)
        original.url should be(resp.url)

        original.description should be(resp.description)
        resp.content should be(content)
        status should be(StatusCodes.OK)
      }
    }

    it("500 error when extract error") {
      val id = feedIds(1)
      val original = feeds.filter(_.id == Some(id)).head

      val req = Get(s"${feedUrl}/${id}") ~> computeRoute

      sourcesRef.expectMsg(1 seconds, ExtractContent(original.sourceId,
        id, original.url))
      sourcesRef.reply(ExtractError("error"))

      req ~> check {
        status should be(StatusCodes.InternalServerError)
      }
    }

    it("return 404 when id not found") {
      Get(s"${feedUrl}/1000") ~> computeRoute ~> check {
        responseAs[String] should be("Feed not found")
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
      val feed = unfavAndUnRead
      val firstNonFavorite = unfavAndUnReadId

      val req = Put(s"${feedUrl}/mark/${firstNonFavorite}") ~> computeRoute
      publishActor.expectMsgAllClassOf(classOf[PublishEvent])

      req ~> check {
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
      val feed = favAndRead
      val favId = feed.id.get
      Put(s"${feedUrl}/unmark/${favId}") ~> computeRoute ~> check {
        val resp = JsonParser(responseAs[String]).convertTo[Feed]
        feed.title should be(resp.title)
        feed.url should be(resp.url)
        status should be(StatusCodes.OK)

        val extract = db withSession { implicit session =>
          driver.query.feeds.filter(_.id === favId).firstOption
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
      val feed = unfavAndUnRead
      val fId = feed.id.get
      Put(s"${feedUrl}/read/${fId}") ~> computeRoute ~> check {
        val resp = JsonParser(responseAs[String]).convertTo[Feed]
        feed.title should be(resp.title)
        feed.url should be(resp.url)
        status should be(StatusCodes.OK)

        val extract = db withSession { implicit session =>
          driver.query.feeds.filter(_.id === fId).firstOption
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
      val feed = favAndRead
      val fId = feed.id.get
      Put(s"${feedUrl}/unread/${fId}") ~> computeRoute ~> check {
        val resp = JsonParser(responseAs[String]).convertTo[Feed]
        feed.title should be(resp.title)
        feed.url should be(resp.url)
        status should be(StatusCodes.OK)

        val extract = db withSession { implicit session =>
          driver.query.feeds.filter(_.id === fId).firstOption
        }
        extract.get.favorite should be(false)
      }
    }
  }





}
*/