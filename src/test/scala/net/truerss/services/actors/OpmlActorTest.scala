package net.truerss.services.actors

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import net.truerss.services.Gen
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
import truerss.api.{BadRequestResponse, ImportResponse, Ok}
import truerss.dto.NewSourceDto
import truerss.services.actors.sync.SourcesKeeperActor
import truerss.services.{OpmlService, SourcesService}
import truerss.util.Outline

import scala.concurrent.Future

class OpmlActorTest extends TestKit(ActorSystem("OpmlActor"))
  with SpecificationLike with Mockito with ActorTestHelper {
/*
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
      val n = Gen.genNewSource
      val v = Gen.genView
      service.parse(any[String]).returns(Right(Iterable(o)))
      sourcesService.addSource(any[NewSourceDto]).returns(f(Right(v)))

      pass(OpmlActor.CreateOpmlFromFile("test")) {
        case msg: ImportResponse =>
          streamRef.expectMsgClass(classOf[SourcesKeeperActor.NewSource])
          msg.result.keys must have size 1

          msg.result.values.head must beRight
      }
    }

    "produce new sources and errors on partially valid opml" in new MyTest {
      val error = "boom"
      val o1 = Outline(
        title = "test title",
        link = Gen.genUrl
      )
      val o2 = Outline(
        title = "test title",
        link = Gen.genUrl
      )
      val n1 = Gen.genNewSource.copy(url = o1.link, name = o1.title)
      val n2 = Gen.genNewSource.copy(url = o2.link, name = o2.title)
      val v = Gen.genView

      service.parse(any[String]).returns(Right(Iterable(o1, o2)))
      sourcesService.addSource(any[NewSourceDto])
        .returns(f(Right(v)))
        .thenReturns(f(Left(List(error))))

      pass(OpmlActor.CreateOpmlFromFile("test")) {
        case msg: ImportResponse =>
          msg.result.keys must have size 2

          msg.result.values.head must beRight
          msg.result.values.last must beLeft
      }
    }

    "return errors, when opml is not valid" in new MyTest {
      val boom = "boom"
      service.parse(any[String]).returns(Left(boom))

      pass(OpmlActor.CreateOpmlFromFile("test")) {
        case msg: BadRequestResponse =>
          msg.msg ==== boom
      }
    }
  }

  private class MyTest extends Scope {
    val me = TestProbe()
    val streamRef = TestProbe()
    val service = mock[OpmlService]
    val sourcesService = mock[SourcesService]

    system.eventStream.subscribe(streamRef.ref, classOf[SourcesKeeperActor.NewSource])

    def pass(msg: Any)(pf: PartialFunction[Any, Unit]) = {
      TestActorRef(new OpmlActor(service, sourcesService)).tell(msg, me.ref)
      me.expectMsgPF()(pf)
    }
  }
  */
}
