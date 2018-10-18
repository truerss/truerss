package net.truerss.services.actors

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import net.truerss.services.Gen
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
import truerss.api._
import truerss.models.{Notify, NotifyLevels}
import truerss.services.actors.{SourcesManagementActor => S}
import truerss.services.{SourcesActor, SourcesService}

import scala.concurrent.duration._

class SourcesManagementActorTest
  extends TestKit(ActorSystem("SourcesManagementActor"))
  with SpecificationLike with Mockito with ActorTestHelper {

  sequential

  val duration = 1 seconds

  "SourceManagementActor" should {
    "get all sources" in new MyTest {
      service.getAll.returns(f(Vector(v)))
      pass(S.GetAll) {
        case msg: SourcesResponse =>
          msg.xs must contain(allOf(v))
      }
    }

    "get one" should {
      "when found" in new MyTest {
        service.getSource(v.id).returns(f(Some(v)))
        pass(S.GetSource(v.id)) {
          case msg: SourceResponse =>
            msg.x must beSome(v)
        }
      }

      "not found" in new MyTest {
        service.getSource(v.id).returns(f(None))
        pass(S.GetSource(v.id)) {
          case msg: SourceResponse =>
            msg.x must beNone
        }
      }
    }

    "mark as read" in {
      "anyway returns ok" in new MyTest {
        service.markAsRead(v.id).returns(f(None))
        pass(S.Mark(v.id)) {
          case msg: Ok =>
            msg.msg ==== "ok"
        }
      }
    }

    "delete source" in {
      "when found" in new MyTest {
        service.delete(v.id).returns(f(Some(v)))
        pass(S.DeleteSource(v.id)) {
          case msg: Ok =>
            streamRef.expectMsgClass(classOf[WSController.SourceDeleted])
            streamRef.expectMsgClass(classOf[SourcesActor.SourceDeleted])
            msg.msg ==== "ok"
        }
      }

      "when not found" in new MyTest {
        service.delete(v.id).returns(f(None))
        pass(S.DeleteSource(v.id)) {
          case msg: NotFoundResponse =>
            streamRef.expectNoMessage(duration)
            success
        }
      }
    }

    "add source" in {
      "when valid" in new MyTest {
        val n = Gen.genNewSource
        service.addSource(n).returns(f(Right(v)))
        pass(S.AddSource(n)) {
          case msg: SourceResponse =>
            streamRef.expectMsgClass(classOf[WSController.SourceAdded])
            streamRef.expectMsgClass(classOf[SourcesActor.NewSource])
            msg.x must beSome(v)
        }
      }

      "when invalid" in new MyTest {
        val error = "boom"
        val n = Gen.genNewSource
        service.addSource(n).returns(f(Left(List(error))))
        pass(S.AddSource(n)) {
          case msg: BadRequestResponse =>
            streamRef.expectNoMessage(duration)
            msg.msg ==== error
        }
      }
    }

    "update source" in {
      "when valid" in new MyTest {
        val id = Gen.genLong
        val u = Gen.genUpdSource(id)
        service.updateSource(id, u).returns(f(Right(v)))
        pass(S.UpdateSource(id, u)) {
          case msg: SourceResponse =>
            streamRef.expectMsgClass(classOf[WSController.SourceUpdated])
            streamRef.expectMsgClass(classOf[SourcesActor.ReloadSource])
            msg.x must beSome(v)
        }
      }

      "when invalid" in new MyTest {
        val error = "boom"
        val id = Gen.genLong
        val u = Gen.genUpdSource(id)
        service.updateSource(id, u).returns(f(Left(List(error))))
        pass(S.UpdateSource(id, u)) {
          case msg: BadRequestResponse =>
            streamRef.expectNoMessage(duration)
            msg.msg ==== error
        }
      }
    }

    "add source: valid/not valid" in new MyTest {
      val error = "boom"
      val n = Gen.genNewSource
      val i = Gen.genNewSource
      service.addSource(n).returns(f(Right(v)))
      service.addSource(i).returns(f(Left(List(error))))
      val msg = S.AddSources(Iterable(n, i))
      val testRef = TestActorRef(new S(service)).tell(msg, me.ref)

      val pf: PartialFunction[Any, Unit] = {
        case _: WSController.SourceAdded =>
          success
        case _: SourcesActor.NewSource =>
          success
        case msg: Notify =>
          msg.level ==== NotifyLevels.Danger
      }

      streamRef.expectMsgPF()(pf)
      streamRef.expectMsgPF()(pf)
      streamRef.expectMsgPF()(pf)
    }
  }

  private class MyTest extends Scope {
    val v = Gen.genView
    val me = TestProbe()
    val stream = system.eventStream
    val streamRef = TestProbe()
    stream.subscribe(streamRef.ref, classOf[WSController.SourceDeleted])
    stream.subscribe(streamRef.ref, classOf[SourcesActor.SourceDeleted])
    stream.subscribe(streamRef.ref, classOf[WSController.SourceAdded])
    stream.subscribe(streamRef.ref, classOf[SourcesActor.NewSource])
    stream.subscribe(streamRef.ref, classOf[WSController.SourceUpdated])
    stream.subscribe(streamRef.ref, classOf[SourcesActor.ReloadSource])
    stream.subscribe(streamRef.ref, classOf[Notify])
    val service = mock[SourcesService]

    def pass(msg: Any)(pf: PartialFunction[Any, Unit]) = {
      TestActorRef(new S(service)).tell(msg, me.ref)
      me.expectMsgPF()(pf)
    }
  }

}
