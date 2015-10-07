import java.util.Date

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

import scala.slick.driver.H2Driver.simple._
import scala.language.postfixOps
import scala.concurrent.duration._

import spray.routing._
import spray.testkit.RouteTest
import spray.json._
import spray.testkit.ScalatestRouteTest
import spray.json.JsonParser

import truerss.models._
import com.github.truerss.base.Entry
import truerss.db.DbActor
import truerss.api._
import truerss.util.ApplicationPlugins

import scala.slick.jdbc.JdbcBackend
import scala.concurrent.Await
import scala.concurrent.duration._


class DbActorTest extends DbHelper with FunSpecLike
  with Matchers with Common with ScalatestRouteTest {

  def actorRefFactory = system
  import Gen._
  import truerss.system.ws.NewFeeds
  import truerss.system.db.AddFeeds
  val wsActor = TestProbe()
  system.eventStream.subscribe(wsActor.ref, classOf[NewFeeds])

  val dbRef = system.actorOf(Props(new DbActor(db, driver)), "test-db")



  describe("New feeds") {
    it("publish new feeds into stream") {
      val source = genSource(None)
      val sourceId = db withSession { implicit session =>
        (driver.query.sources returning driver.query.sources.map(_.id)) += source
      }

      val baseUrl = "http://example.com"

      def genEntry(index: Int): Entry = Entry(
        url = s"$baseUrl/$index",
        title = s"title-$index",
        author = "author",
        publishedDate = new Date(),
        description = Some(s"description-$index"),
        content = Some(s"content-$index")
      )

      val feeds = (1 to 10).map { index =>
        genEntry(index)
      }.toVector

      dbRef ! AddFeeds(sourceId, feeds)

      wsActor.expectMsgAllClassOf(classOf[NewFeeds])

      // then force update it
      val updFeeds = (1 to 4).map { index =>
        genEntry(index).copy(content = Some(s"new-content-$index"),
          forceUpdate = true)
      }.toVector

      dbRef ! AddFeeds(sourceId, updFeeds)

      Thread.sleep(3000)

      val f = db withSession { implicit session =>
        driver.query.feeds.filter(_.sourceId === sourceId).build
      }

      f.toVector.count(_.content.get.contains("new-")) should be(4)

    }
  }

  override def afterAll = {
    super.afterAll()
  }


}
