package net.truerss.services

import net.truerss.Gen
import net.truerss.dao.FullDbHelper
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import truerss.db.Feed
import truerss.dto.SearchRequest
import truerss.services.SearchService

import scala.concurrent.duration._

class SearchServiceTest(implicit ee: ExecutionEnv)
  extends FullDbHelper with SpecificationLike {

  import SearchServiceTest._

  override def dbName = "search_service_test"

  sequential

  val dao = dbLayer.feedDao

  val service = new SearchService(dbLayer)

  val sourceId1 = 1
  val sourceId2 = 2
  val feed1 = Gen.genFeed(sourceId1, Gen.genUrl).copy(favorite = true)
    .withText("test")
  val feedId1 = a(dao.insert(feed1))
  val feed2 = Gen.genFeed(sourceId1, Gen.genUrl).copy(favorite = false)
    .withText("foo")
  val feedId2 = a(dao.insert(feed2))
  val feed3 = Gen.genFeed(sourceId2, Gen.genUrl).copy(favorite = true)
    .withText("bar")
  val feedId3 = a(dao.insert(feed3))
  val feed4 = Gen.genFeed(sourceId2, Gen.genUrl).copy(favorite = false)
    .withText("baz-test")
  val feedId4 = a(dao.insert(feed4))

  "SearchService" should {
    "find with favorites" in {
      service.search(SearchRequest(true, "test"))
        .map(xs => xs.map(_.id))
          .map { xs => xs must contain(exactly(feedId1)) }.await

      service.search(SearchRequest(true, "ba"))
        .map(xs => xs.map(_.id))
        .map { xs => xs must contain(exactly(feedId3)) }.await


      service.search(SearchRequest(true, "foo"))
        .map(xs => xs.map(_.id))
        .map { xs =>
          xs must be empty
        }.await

    }

    "find without favorites" in {
      service.search(SearchRequest(false, "test"))
        .map(xs => xs.map(_.id))
        .map { xs => xs must contain(exactly(feedId1, feedId4)) }.await

      service.search(SearchRequest(false, "ba"))
        .map(xs => xs.map(_.id))
        .map { xs => xs must contain(exactly(feedId3, feedId4)) }.await


      service.search(SearchRequest(false, "foo"))
        .map(xs => xs.map(_.id))
        .map { xs =>
          xs must contain(exactly(feedId2))
        }.await

      service.search(SearchRequest(false, "boom"))
        .map(xs => xs.map(_.id))
        .map { xs =>
          xs must be empty
        }.await
    }
  }

}

object SearchServiceTest {
  implicit class FeedTestExt(val x: Feed) extends AnyVal {
    def withText(str: String): Feed = {
      x.copy(
        title = str,
        author = str,
        url = str,
        description = Some(str),
        normalized = str,
        content = Some(str)
      )
    }
  }
}