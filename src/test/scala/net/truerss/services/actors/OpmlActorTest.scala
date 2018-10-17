package net.truerss.services.actors

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import net.truerss.services.Gen
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
import truerss.api.{BadRequestResponse, Ok}
import truerss.models.Notify
import truerss.services.OpmlService
import truerss.services.actors.{AddSourcesActor, OpmlActor}
import truerss.util.Outline

import scala.concurrent.Future

class OpmlActorTest extends TestKit(ActorSystem("OpmlActor"))
  with SpecificationLike with Mockito {

  sequential

  "OpmlActor" should {
    "return opml" in new MyTest {
      val test = "test"
      service.build.returns(Future.successful(test))
      pass(OpmlActor.GetOpml) {
        case msg: Ok =>
          msg.msg ==== test
      }
    }

    "produce new sources on valid opml" in new MyTest {
      val o = Outline(
        title = "test title",
        link = Gen.genUrl
      )
      service.parse(any[String]).returns(Right(Iterable(o)))

      pass(OpmlActor.CreateOpmlFromFile("test")) {
        case msg: Ok =>
          streamRef.expectMsgPF() {
            case sources: AddSourcesActor.AddSources =>
              sources.xs must have size 1
          }
      }
    }

    "return errors, when opml is not valid" in new MyTest {
      val boom = "boom"
      service.parse(any[String]).returns(Left(boom))

      pass(OpmlActor.CreateOpmlFromFile("test")) {
        case msg: BadRequestResponse =>
          streamRef.expectMsgClass(classOf[Notify])
          msg.msg ==== boom
      }
    }
  }

  private class MyTest extends Scope {
    val me = TestProbe()
    val streamRef = TestProbe()
    system.eventStream.subscribe(streamRef.ref, classOf[Notify])
    system.eventStream.subscribe(streamRef.ref, classOf[AddSourcesActor.AddSources])

    val service = mock[OpmlService]

    def pass(msg: Any)(pf: PartialFunction[Any, Unit]) = {
      TestActorRef(new OpmlActor(service)).tell(msg, me.ref)
      me.expectMsgPF()(pf)
    }
  }

}
