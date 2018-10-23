package net.truerss.services.actors

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import net.truerss.services.Gen
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
import truerss.api._
import truerss.services.DbHelperActor.FeedContentUpdate
import truerss.services.actors.management.FeedsManagementActor
import truerss.services.{ContentReaderService, FeedsService, PublishPluginActor}

import scala.concurrent.duration._

class FeedsManagementActorTest extends TestKit(ActorSystem("FeedsManagementActor"))
  with SpecificationLike with Mockito with ActorTestHelper {

  sequential

  val duration = 1 seconds

  "FeedsManagementActor" should {
    "mark all" in new MyTest {
      service.markAllAsRead.returns(f(1))
      pass(FeedsManagementActor.MarkAll) {
        case _: Ok =>
          success
      }
    }

    "mark as favorites" in new MyTest {
      service.addToFavorites(fId).returns(f(Some(feed)))
      pass(FeedsManagementActor.MarkFeed(fId)) {
        case msg: FeedResponse =>
          streamRef.expectMsgClass(classOf[PublishPluginActor.PublishEvent])
          msg.x.id ==== fId
      }
    }

    "mark as favorites: not found" in new MyTest {
      service.addToFavorites(fId).returns(f(None))
      pass(FeedsManagementActor.MarkFeed(fId)) {
        case msg: NotFoundResponse =>
          success
      }
    }

    "mark as unfavorites" in new MyTest {
      service.removeFromFavorites(fId).returns(f(Some(feed)))
      pass(FeedsManagementActor.UnmarkFeed(fId)) {
        case msg: FeedResponse =>
          msg.x.id ==== fId
      }
    }

    "mark as read" in new MyTest {
      service.markAsRead(fId).returns(f(Some(feed)))
      pass(FeedsManagementActor.MarkAsReadFeed(fId)) {
        case msg: FeedResponse =>
          msg.x.id ==== fId
      }
    }

    "mark as unread" in new MyTest {
      service.markAsUnread(fId).returns(f(Some(feed)))
      pass(FeedsManagementActor.MarkAsUnreadFeed(fId)) {
        case msg: FeedResponse =>
          msg.x.id ==== fId
      }
    }

    "gen unread" in new MyTest {
      service.findUnread(sourceId).returns(f(Vector(feed)))
      pass(FeedsManagementActor.Unread(sourceId)) {
        case msg: FeedsResponse =>
          msg.xs must have size 1
          msg.xs.head.id ==== fId
      }
    }

    "get for source" in new MyTest {
      val count = 100
      val o = 0
      val l = 10
      service.findBySource(sourceId, o, l).returns(f(Vector(feed), count))
      pass(FeedsManagementActor.ExtractFeedsForSource(sourceId, o, l)) {
        case msg: FeedsPageResponse =>
          msg.total ==== count
          msg.xs must contain(allOf(feed))
      }
    }

    "get latest" in new MyTest {
      val count = 10
      service.latest(count).returns(f(Vector(feed)))
      pass(FeedsManagementActor.Latest(count)) {
        case msg: FeedsResponse =>
          msg.xs must contain(allOf(feed))
      }
    }

    "get favorites" in new MyTest {
      service.favorites.returns(f(Vector(feed)))
      pass(FeedsManagementActor.Favorites) {
        case msg: FeedsResponse =>
          msg.xs must contain(allOf(feed))
      }
    }

    "get feed" should {
      "not found on non-existing feed" in new MyTest {
        service.findOne(fId).returns(f(None))
        pass(FeedsManagementActor.GetFeed(fId)) {
          case msg: NotFoundResponse =>
            success
        }
      }

      "internal error when something went wrong with content reader" in new MyTest {
        val error = "boom"
        val fn = feed.copy(content = None)
        service.findOne(fId).returns(f(Some(fn)))
        contentReaderService.read(anyString).returns(Left(error))
        pass(FeedsManagementActor.GetFeed(fId)) {
          case msg: InternalServerErrorResponse =>
            msg.msg ==== error
        }
      }

      "ok, not call service when content already present" in new MyTest {
        val c = "content"
        val fn = feed.copy(content = Some(c))
        service.findOne(fId).returns(f(Some(fn)))
        there was no(contentReaderService).read(anyString)
        pass(FeedsManagementActor.GetFeed(fId)) {
          case msg: FeedResponse =>
            streamRef.expectNoMessage(duration)
            msg.x.content must beSome(c)
        }
      }

      "call service when feed without content" in new MyTest {
        val c = "content"
        val fn = feed.copy(content = None)
        service.findOne(fId).returns(f(Some(fn)))
        contentReaderService.read(anyString).returns(Right(Some(c)))
        pass(FeedsManagementActor.GetFeed(fId)) {
          case msg: FeedResponse =>
            streamRef.expectMsgClass(classOf[FeedContentUpdate])
            msg.x.content must beSome(c)
        }
      }
    }
  }


  private class MyTest extends Scope {
    val sourceId = Gen.genLong
    val fId = Gen.genLong
    val feed = Gen.genFeedDto.copy(id = fId, sourceId = sourceId)
    val v = Gen.genFeed(1, Gen.genUrl)
    val me = TestProbe()
    val stream = system.eventStream
    val streamRef = TestProbe()
    stream.subscribe(streamRef.ref, classOf[PublishPluginActor.PublishEvent])
    stream.subscribe(streamRef.ref, classOf[FeedContentUpdate])
    val service = mock[FeedsService]
    val contentReaderService = mock[ContentReaderService]

    def pass(msg: Any)(pf: PartialFunction[Any, Unit]) = {
      TestActorRef(new FeedsManagementActor(service, contentReaderService)).tell(msg, me.ref)
      me.expectMsgPF()(pf orElse {
        case _ => failure
      })
    }
  }

}
