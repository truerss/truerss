package net.truerss.dao

import java.util.Date

import com.github.truerss.base.Entry
import net.truerss.Gen
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.{BeforeAfterAll, Scope}
import truerss.db.{Feed, Source}
import truerss.services.SourceOverviewService

import scala.concurrent.Await
import scala.concurrent.duration._

class FeedDaoTest(implicit ee: ExecutionEnv)
  extends FullDbHelper with SpecificationLike with BeforeAfterAll {

  import SourceOverviewService._
  import net.truerss.FutureTestExt._

  override def dbName = "fee_dao_spec"

  sequential

  private val SourceIds = Iterator.from(1)

  implicit val duration = 3 seconds

  val feedDao = dbLayer.feedDao
  val sourceDao = dbLayer.sourceDao

  "feed dao" should {
    "find unread" in new FeedsSpec(3, readStatus = false) {
      feedDao.findUnread(sourceId) ~> { _ must contain(atLeast(feeds : _*)) }
      feedDao.findBySource(sourceId) ~> { feedOpt =>
        feedOpt must not be empty

        val feedId = feedOpt.head.id.get

        feedDao.modifyRead(feedId, true) ~> { _ must be_==(1) }
        feedDao.findUnread(sourceId) ~> { _ must contain(atLeast(feeds.slice(1, 3) : _*)) }
      }
    }

    "delete feeds" in new FeedsSpec(10) {
      feedDao.feedCountBySourceId(sourceId, false) ~> { _ must be_==(10) }

      feedDao.deleteFeedsBySource(sourceId) ~> { _ must be_==(10) }

      feedDao.feedCountBySourceId(sourceId, false) ~> { _ must be_==(0) }
    }

    "mark all" in new FeedsSpec(9, false) {
      val anotherSourceId = Gen.genLong
      insertSource(Gen.genSource(Some(anotherSourceId)))
      insertFeed(Gen.genFeed(anotherSourceId, "some-url").copy(read = false))

      // 9 + 1 + X = 10 + from another specs
      feedDao.markAll ~> { _ must be_>=(10) }

      feedDao.findUnread(sourceId).map(_.size) ~> { _ must be_==(0) }

      feedDao.findUnread(anotherSourceId).map(_.size) ~> { _ must be_==(0) }
    }

    "mark by source" in new FeedsSpec(10, readStatus = false) {
      val anotherSourceId = SourceIds.next().toLong
      insertSource(Gen.genSource(Some(anotherSourceId)))
      insertFeed(Gen.genFeed(anotherSourceId, "some-url").copy(read = false))

      feedDao.markBySource(sourceId) ~> { _ must be_==(10) }

      feedDao.findUnread(sourceId).map(_.size) ~> { _ must be_==(0) }

      feedDao.findUnread(anotherSourceId).map(_.size) ~> { _ must be_==(1) }
    }

    "last n" in new FeedsSpec(10, false) {
      feedDao.lastN(0, 1) ~> { x =>
        x._1 must have size 1

        x._2 must be_>=(10) // from antother specs
      }

      feedDao.lastN(3, 7) ~> { x =>
        x._1 must have size 7

        x._2 must be_>=(feeds.size)
      }
    }

    "page for source" in new FeedsSpec(10, readStatus = false) {
      val n = feeds.sortBy(_.publishedDate).slice(7, 100)
      feedDao.pageForSource(sourceId, false, 7, 100).map { xs =>
        xs._1.flatMap(_.id)
      } ~> { _ must contain(allOf(n.flatMap(_.id) : _*)) }

      feedDao.pageForSource(sourceId, false, 0, 3) ~> { xs =>
        xs._1 must have size 3

        xs._2 ==== 10
      }

      feedDao.pageForSource(sourceId, false, 3, 10) ~> { xs =>
        xs._1 must have size 7

        xs._2 ==== 10
      }

      feedDao.pageForSource(sourceId, false, 10, 100) ~> { xs =>
        xs._1 must be empty

        xs._2 ==== 10
      }
    }

    "favorites" in new FeedsSpec(3) {
      feedDao.modifyFav(ids.head, true) ~> { _ must be_==(1) }
      feedDao.favorites(0, 100).map(_._1.map(_.id)) ~> { _ must contain(feeds.head.id) }
    }

    "update content" in new FeedsSpec(1) {
      val feedId = ids.head

      val content = "foo bar baz"

      feedDao.updateContent(feedId, content) ~> { _ must be_==(1) }

      feedDao.findOne(feedId).map(x => x.map(_.content)) ~> { _ must beSome(Option(content)) }
    }

    "feeds by source" in new FeedsSpec(1, readStatus = false) {
      feedDao.feedBySourceCount(false).map{ xs =>
        xs.toMap.get(sourceId)
      } ~> { _ must beSome(1) }
    }

    "merge feeds - update old feeds and insert new" in new FeedsSpec(3) {
      val feed = feeds.head
      val entry = Entry(
        url = feed.url,
        title = feed.title,
        author = feed.author,
        publishedDate = new Date(),
        description = Option("new description for feed"),
        content = Some("new content"),
        forceUpdate = true
      )

      val newEntry = Entry(
        url = s"${source.url}/new-entry",
        title = "some-title",
        author = feed.author,
        publishedDate = new Date(),
        description = Some("abc"),
        content = None,
        forceUpdate = true
      )

      feedDao.mergeFeeds(sourceId, entry :: newEntry :: Nil)
        .map(xs => xs.size) ~> { _ must be_==(1) }

      feedDao.findOne(ids.head)
        .map(x => x.map(_.description)) ~> { _ must beSome(entry.description) }
    }

  }


  private class FeedsSpec(count: Int = 0, readStatus: Boolean = true) extends Scope {
    private val tmpSource = Gen.genSource()
    val sourceId = SourceIds.next.toLong
    insertSource(tmpSource.copy(id = Some(sourceId)))
    val source = tmpSource.copy(id = Some(sourceId))

    val ids = scala.collection.mutable.ArrayBuffer[Long]()

    val feeds = (0 until count).map { index =>
      val feed = Gen.genFeed(sourceId, source.url).copy(read = readStatus)
        .copy(publishedDate = Gen.now.plusDays(index))
      val tmpId = insertFeed(feed)
      ids += tmpId
      feed.copy(id = Some(tmpId))
    }
  }


  def insertSource(source: Source = Gen.genSource()) = {
    Await.result(sourceDao.insertMany(source :: Nil), duration)
  }

  def insertFeed(feed: Feed) = {
    Await.result(feedDao.insert(feed), duration)
  }


}
