import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}
import truerss.controllers.ModelsResponse
import truerss.plugins.DefaultSiteReader
import truerss.system.SourcesActor

import scala.concurrent.duration._
import scalaz.Scalaz._
import scalaz._
/**
 * Created by mike on 24.8.15.
 */
class SourcesActorTest(_system: ActorSystem) extends TestKit(_system)
  with FunSpecLike with Matchers  with ImplicitSender
  with DbHelper with BeforeAndAfterAll{

  def this() = this(ActorSystem("SourcesActorTest"))

  implicit val timeout = Timeout(10 seconds)

  import Gen._

  override def beforeAll = {

  }
  override def afterAll = {
    system.shutdown()
  }

  val dbRef = TestProbe()
  val networkRef = TestProbe()
  val sysActor = TestProbe()
  val sourcesRef = system.actorOf(Props(new SourcesActor(
    truerss.util.ApplicationPlugins(),
    sysActor.ref,
    networkRef.ref)), "sources")

  import truerss.system.db.{OnlySources, AddFeeds}
  import truerss.system.util.{SourceLastUpdate, UpdateOne}
  import truerss.system.network._

  val stream = system.eventStream
  stream.subscribe(dbRef.ref, classOf[SourceLastUpdate])
  stream.subscribe(dbRef.ref, classOf[AddFeeds])

  sysActor.expectMsg(4 seconds, OnlySources)
  val source1 = genSource(1L.some)
  sysActor.reply(Vector(source1, genSource(2L.some),
    genSource(3L.some)))
  networkRef.expectMsgAllClassOf(1 seconds, classOf[NetworkInitialize])


  describe("update source") {
    it ("update source should update lastUpdate field in db") {
      sourcesRef ! UpdateOne(1L)
      dbRef.expectMsg(1 seconds, SourceLastUpdate(1L))
      networkRef.expectMsg(2 seconds, Grep(1L, source1.url))
      networkRef.reply(ExtractedEntries(1L, Vector.empty))
      dbRef.expectMsg(1 seconds, AddFeeds(1L, Vector.empty))
    }
  }

}
