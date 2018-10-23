package net.truerss.services

import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.Scope
import truerss.services.FeedsService

class FeedsServiceTest(implicit ee: ExecutionEnv)
  extends FullDbHelper with SpecificationLike {

  override def dbName = "feeds_service_test"

  sequential

  val dao = dbLayer.feedDao

  val service = new FeedsService(dbLayer)

  "FeedService" should {
    "mark all as read" in new MyTest {
      val f = Gen.genFeed(sourceId, Gen.genUrl).copy(read = false)
      val feedId = a(dao.insert(f))

      service.markAllAsRead.map(_ ==== 1).await

      dao.findOne(feedId).map { x =>
        x must beSome
        x.get.read must beTrue
      }.await
    }

    "add/remove ~ favorites" in new MyTest {
      val f = Gen.genFeed(sourceId, Gen.genUrl).copy(favorite = false)
      val feedId = a(dao.insert(f))

      service.addToFavorites(feedId)

      w

      dao.findOne(feedId).map { x =>
        x.get.favorite must beTrue
      }.await

      service.removeFromFavorites(feedId)

      w

      dao.findOne(feedId).map { x =>
        x.get.favorite must beFalse
      }.await
    }

    "mark/unmark ~ read" in new MyTest {
      val f = Gen.genFeed(sourceId, Gen.genUrl).copy(read = false)
      val feedId = a(dao.insert(f))

      service.markAsRead(feedId)

      w

      dao.findOne(feedId).map { x =>
        x.get.read must beTrue
      }.await

      service.markAsUnread(feedId)

      w

      dao.findOne(feedId).map { x =>
        x.get.read must beFalse
      }.await
    }

    "find all unread, find by source, favorites" in new MyTest {
      val f1 = Gen.genFeed(sourceId, Gen.genUrl).copy(read = false, favorite = true)
      val f2 = Gen.genFeed(sourceId, Gen.genUrl).copy(read = true, favorite = false)
      val f3 = Gen.genFeed(sourceId, Gen.genUrl).copy(read = false, favorite = true)
      val f4 = Gen.genFeed(sourceId, Gen.genUrl).copy(read = false, favorite = true)

      val id1 = a(dao.insert(f1))
      val id2 = a(dao.insert(f2))
      val id3 = a(dao.insert(f3))
      val id4 = a(dao.insert(f4))

      service.findUnread(sourceId).map { xs =>
        xs must have size 3
        xs.map(_.id) must contain(allOf(id1, id3, id4))
      }

      service.findBySource(sourceId, 0, 1).map { tpl =>
        val xs = tpl._1
        xs must have size 1
        tpl._2 ==== 4
      }.await

      service.findBySource(sourceId, 10, 1).map { tpl =>
        val xs = tpl._1
        xs must be empty

        tpl._2 ==== 4
      }.await

      service.favorites.map { xs =>
        xs must have size 3
        xs.map(_.id) must contain(allOf(id1, id3, id4))
      }
    }
  }

  private val it = Iterator.from(1)

  private class MyTest extends Scope {
    val sourceId = it.next().toLong
    val source = Gen.genSource(Some(sourceId))

    insert(source)
  }

}
