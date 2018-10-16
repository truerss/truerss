package net.truerss.services.actors

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import net.truerss.services.Gen
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
import truerss.api.{Ok, SourceResponse, SourcesResponse}
import truerss.services.SourcesService
import truerss.services.actors.{SourcesManagementActor => S}
import truerss.util.Util.ResponseHelpers

import scala.concurrent.Future

class SourcesManagementActorTest
  extends TestKit(ActorSystem("SourcesManagementActor"))
  with SpecificationLike with Mockito {

  "SourceManagementActor" should {
    "get all sources" in new MyTest {
      val v = Gen.genView
      service.getAll.returns(f(Vector(v)))
      pass(S.GetAll) {
        case msg: SourcesResponse =>
          msg.xs must contain(allOf(v))
      }
    }

    "get one" should {
      "when found" in new MyTest {
        val v = Gen.genView
        service.getSource(v.id).returns(f(Some(v)))
        pass(S.GetSource(v.id)) {
          case msg: SourceResponse =>
            msg.x must beSome(v)
        }
      }

      "not found" in new MyTest {
        service.getSource(any[Long]).returns(f(None))
        pass(S.GetSource(1L)) {
          case msg: SourceResponse =>
            msg.x must beNone
        }
      }
    }

    "mark as read" in {
      "anyway returns ok" in new MyTest {
        service.markAsRead(any[Long]).returns(f(None))
        pass(S.Mark(1L)) {
          case msg: Ok =>
            msg.msg ==== "ok"
        }
      }
    }
  }

  private def f[T](x: T): Future[T] = Future.successful(x)

  private class MyTest extends Scope {
    val me = TestProbe()
    val service = mock[SourcesService]

    def spawn(x: SourcesService) = {
      TestActorRef(new S(x))
    }

    def pass(msg: Any)(pf: PartialFunction[Any, Unit]) = {
      spawn(service).tell(msg, me.ref)
      me.expectMsgPF()(pf)
    }
  }

}
