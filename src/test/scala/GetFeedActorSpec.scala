
import akka.actor._
import akka.testkit._
import akka.pattern.ask
import akka.util.Timeout
import org.specs2.mock.Mockito
import org.specs2.specification.Scope
import org.specs2.specification.AfterAll
import org.specs2.mutable.SpecificationLike
import truerss.api.{BadRequestResponse, InternalServerErrorResponse, ModelResponse, NotFoundResponse}
import truerss.system.actors.{AddOrUpdateFSMHelpers, AddSourceFSM, GetFeedActor}
import truerss.system.db._
import truerss.system.network.{ExtractContent, ExtractContentForEntry, ExtractError, SourceNotFound}
import truerss.system.util.{FeedContentUpdate, NewSource}
import truerss.system.ws.SourceAdded
import truerss.util.ApplicationPlugins

import scala.concurrent.duration._
import scala.concurrent.Await

class GetFeedActorSpec extends TestKit(ActorSystem("getFeedActorSpec"))
  with SpecificationLike with AfterAll {

  val duration = 10 seconds

  "get feed actor" should {
    "404 when feed not found" in new ActorScope {
      actor.tell(GetFeed(1L), me.ref)
      dbRef.expectMsg(duration, GetFeed(1L))
      dbRef.reply(ResponseMaybeFeed(None))
      me.expectMsg(NotFoundResponse("Feed not found"))
    }

    "return feed when feed have content" in new ActorScope {
      val f = Gen.genFeed(1L, "").copy(content = Some("foo"))
      actor.tell(GetFeed(1L), me.ref)
      dbRef.expectMsg(duration, GetFeed(1L))
      dbRef.reply(ResponseMaybeFeed(Some(f)))
      me.expectMsg(duration, ModelResponse(f))
    }

    "500 when impossible fetch content" in new ActorScope {
      val id = 1L
      val f = Gen.genFeed(1L, "").copy(content = None, id = Some(id))
      actor.tell(GetFeed(id), me.ref)
      dbRef.expectMsg(duration, GetFeed(1L))
      dbRef.reply(ResponseMaybeFeed(Some(f)))
      sourcesRef.expectMsg(duration, ExtractContent(f.sourceId, id, f.url))
      sourcesRef.reply(ExtractError("foo"))
      me.expectMsg(duration, InternalServerErrorResponse("foo"))
    }

    "500 when source for feed not found" in new ActorScope {
      val id = 1L
      val f = Gen.genFeed(1L, "").copy(content = None, id = Some(id))
      actor.tell(GetFeed(id), me.ref)
      dbRef.expectMsg(duration, GetFeed(1L))
      dbRef.reply(ResponseMaybeFeed(Some(f)))
      sourcesRef.expectMsg(duration, ExtractContent(f.sourceId, id, f.url))
      sourcesRef.reply(SourceNotFound(f.sourceId))
      me.expectMsg(duration, InternalServerErrorResponse(s"source ${f.sourceId} not found"))
    }

    "return feed when handler return feed without content" in new ActorScope {
      val id = 1L
      val f = Gen.genFeed(1L, "").copy(content = None, id = Some(id))
      actor.tell(GetFeed(1L), me.ref)
      dbRef.expectMsg(duration, GetFeed(1L))
      dbRef.reply(ResponseMaybeFeed(Some(f)))
      sourcesRef.expectMsg(duration, ExtractContent(f.sourceId, id, f.url))
      sourcesRef.reply(ExtractContentForEntry(f.sourceId, id, None))
      me.expectMsg(duration, ModelResponse(f))
    }

    "return feed with content and update content in db" in new ActorScope {
      val id = 1L
      val content = "foo"
      val f = Gen.genFeed(1L, "").copy(content = None, id = Some(id))
      actor.tell(GetFeed(1L), me.ref)
      dbRef.expectMsg(duration, GetFeed(1L))
      dbRef.reply(ResponseMaybeFeed(Some(f)))
      sourcesRef.expectMsg(duration, ExtractContent(f.sourceId, id, f.url))
      sourcesRef.reply(ExtractContentForEntry(f.sourceId, id, Some(content)))
      stream.expectMsg(duration, FeedContentUpdate(id, content))
      me.expectMsg(duration, ModelResponse(f.copy(content = Some(content))))
    }



  }


  class ActorScope extends Scope {
    val me = TestProbe()
    val dbRef = TestProbe()
    val sourcesRef = TestProbe()
    val stream = TestProbe()

    system.eventStream.subscribe(stream.ref, classOf[FeedContentUpdate])

    val actor = TestActorRef(GetFeedActor.props(dbRef.ref, sourcesRef.ref))

  }

  override def afterAll = {
    system.terminate()
  }

}
