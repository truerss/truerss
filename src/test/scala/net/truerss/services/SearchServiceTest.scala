package net.truerss.services

import net.truerss.Gen
import net.truerss.dao.FullDbHelper
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import truerss.db.Feed
import truerss.dto.SearchRequest
import truerss.services.SearchService

import scala.concurrent.Future

class SearchServiceTest(implicit ee: ExecutionEnv)
  extends FullDbHelper with SpecificationLike {

  import SearchServiceTest._
  import net.truerss.FutureTestExt._

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
      find(SearchRequest(true, "test")) ~> { xs =>
        xs must contain(exactly(feedId1))
      }

      find(SearchRequest(true, "test", 10, 10)) ~> { xs =>
        xs must be empty
      }

      find(SearchRequest(true, "ba")) ~> { xs =>
        xs must contain(exactly(feedId3))
      }

      find(SearchRequest(true, "foo")) ~> { xs =>
        xs must be empty
      }
    }

    "find without favorites" in {
      find(SearchRequest(false, "test")) ~> { xs =>
        xs must contain(exactly(feedId1, feedId4))
      }

      find(SearchRequest(false, "ba")) ~> { xs =>
        xs must contain(exactly(feedId3, feedId4))
      }

      find(SearchRequest(false, "ba", 1, 2)) ~> { xs =>
        xs must contain(exactly(feedId4))
      }

      find(SearchRequest(false, "foo")) ~> { xs =>
        xs must contain(exactly(feedId2))
      }

      find(SearchRequest(false, "boom")) ~> { xs =>
        xs must be empty
      }
    }
  }

  private def find(req: SearchRequest): Future[Vector[Long]] = {
    service.search(req)
      .map(xs => xs._1.map(_.id))
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