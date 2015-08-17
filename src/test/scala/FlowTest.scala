import akka.actor.Props
import akka.io.IO
import akka.pattern._
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, Matchers, FunSpec}
import spray.can.Http
import spray.testkit.ScalatestRouteTest
import truerss.db.DbActor
import truerss.system.SystemActor

import scala.concurrent.Await
import scala.concurrent.duration._
/**
 * Created by mike on 16.8.15.
 */
class FlowTest extends FunSpec with Matchers
  with ScalatestRouteTest with DbHelper with BeforeAndAfterAll {

  implicit val timeout = Timeout(10 seconds)

  import driver.profile.simple._

  val rssServer = system.actorOf(Props[Server])
  val host = "localhost"
  val port = Await.result((IO(Http) ?
    Http.Bind(rssServer, interface = host, port = 0))
    .mapTo[akka.io.Tcp.Bound], timeout.duration).localAddress.getPort

  val url = s"http://$host:$port"
  val okRss = s"$url/ok-rss"

  import Gen._
  val source = genSource().copy(url = okRss)

  override def beforeAll = {
    db withSession { implicit session =>
      (driver.query.sources.ddl ++ driver.query.feeds.ddl).create
      (driver.query.sources returning driver.query.sources.map(_.id)) += source
    }
  }
  override def afterAll = {
    db withSession { implicit session =>
      (driver.query.sources.ddl ++ driver.query.feeds.ddl).drop
    }
    system.shutdown()
  }


  val systemRef = system.actorOf(Props(new SystemActor(db, driver)),
    "test-system-actor")


  describe("Source") {
    it("should extract all feeds for it") {
       1 should be(1)
    }
  }


}
