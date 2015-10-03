import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.Timeout
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}
import truerss.controllers.ModelsResponse
import truerss.plugins.DefaultSiteReader
import truerss.system.SourcesActor

import scala.concurrent.duration._

class SourcesActorTest(_system: ActorSystem) extends TestKit(_system)
  with FunSpecLike with Matchers  with ImplicitSender
  with DbHelper with BeforeAndAfterAll{

  def this() = this(ActorSystem("SourcesActorTest"))

  implicit val timeout = Timeout(10 seconds)

  import Gen._

  override def beforeAll() = {

  }
  override def afterAll() = {
    system.terminate()
  }

  val dbRef = TestProbe()
  val sysActor = TestProbe()
  val sourcesRef = system.actorOf(Props(new SourcesActor(
    truerss.util.ApplicationPlugins(),
    sysActor.ref)), "sources")

  import truerss.system.db.{OnlySources, AddFeeds}
  import truerss.system.util.{SourceLastUpdate, UpdateOne}
  import truerss.system.network._

  val stream = system.eventStream
  stream.subscribe(dbRef.ref, classOf[SourceLastUpdate])
  stream.subscribe(dbRef.ref, classOf[AddFeeds])

  sysActor.expectMsg(4 seconds, OnlySources)
  val source1 = genSource(Some(1L))
  sysActor.reply(Vector(source1, genSource(Some(2L)),
    genSource(Some(3L))))


  describe("update source") {
    it ("update source should update lastUpdate field in db") {
      //sourcesRef ! UpdateOne(1L)
      //dbRef.expectMsg(3 seconds, SourceLastUpdate(1L))
      //dbRef.expectMsg(10 seconds, AddFeeds(1L, Vector.empty))
    }
  }

}
