
import java.net.URL

import akka.actor._
import akka.testkit._
import akka.pattern.ask
import akka.util.Timeout
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import org.specs2.specification.AfterAll
import org.specs2.mutable.SpecificationLike
import truerss.api.{BadRequestResponse, ModelResponse}
import truerss.system.actors.{AddOrUpdateFSMHelpers, AddSourceFSM}
import truerss.system.db._
import truerss.system.util.NewSource
import truerss.system.ws.SourceAdded
import truerss.util.ApplicationPlugins

import scala.concurrent.duration._
import scala.concurrent.Await

class AddSourceFSMSpec extends TestKit(ActorSystem("addSourceSpec"))
  with SpecificationLike with AfterAll with Mockito
  with AddOrUpdateFSMHelpers {

  import system.dispatcher

  val url = new URL("http://example.com")

  def g = Gen.genSource().copy(url = url.toString)

  implicit val timeout = Timeout(10 seconds)
  val duration = timeout.duration

  "add source fsm" should {
    "failed when interval is negative" in new FSMScope {
      val source = g.copy(interval = -10)
      val result = Await.result(fsm ? AddSource(source), duration)
      result ==== BadRequestResponse("Interval must be great than 0")
    }

    "failed when url and name already present in system" in new FSMScope {
      val source = g
      fsm.tell(AddSource(source), me.ref)
      dbRef.expectMsg(duration, UrlIsUniq(source.url, None))
      dbRef.reply(ResponseFeedCheck(1))
      dbRef.expectMsg(duration, NameIsUniq(source.name, None))
      dbRef.reply(ResponseFeedCheck(1))
      me.expectMsg(duration, BadRequestResponse(s"${urlError(source)}, ${nameError(source)}"))
    }

    "failed when name already present in system" in new FSMScope {
      val source = g
      fsm.tell(AddSource(source), me.ref)
      dbRef.expectMsg(duration, UrlIsUniq(source.url, None))
      dbRef.reply(ResponseFeedCheck(0))
      dbRef.expectMsg(duration, NameIsUniq(source.name, None))
      dbRef.reply(ResponseFeedCheck(1))
      me.expectMsg(duration, BadRequestResponse(s"${nameError(source)}"))
    }

    "create new source actor" in new FSMScope {
      val source = g
      val id = 1L
      fsm.tell(AddSource(source), me.ref)
      dbRef.expectMsg(duration, UrlIsUniq(source.url, None))
      dbRef.reply(ResponseFeedCheck(0))
      dbRef.expectMsg(duration, NameIsUniq(source.name, None))
      dbRef.reply(ResponseFeedCheck(0))
      dbRef.expectMsg(duration, AddSource(source))
      dbRef.reply(ResponseSourceId(id))
      val n = source.copy(id = Some(id))
      stream.expectMsg(duration, SourceAdded(n))
      sourcesRef.expectMsg(duration, NewSource(n))
      me.expectMsg(duration, ModelResponse(n))
    }
  }


  class FSMScope extends Scope {
    val me = TestProbe()
    val dbRef = TestProbe()
    val sourcesRef = TestProbe()
    val stream = TestProbe()

    system.eventStream.subscribe(stream.ref, classOf[SourceAdded])

    val appPlugins = mock[ApplicationPlugins]

    appPlugins.matchUrl(url) returns false

    val fsm = TestFSMRef(new AddSourceFSM(dbRef.ref,
      sourcesRef.ref,
      appPlugins
    ))
  }

  override def afterAll(): Unit = {
    system.terminate()
  }

}
