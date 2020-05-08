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

  override def dbName = "fee_dao_spec"

  sequential

  implicit val duration = 3 seconds

  val feedDao = dbLayer.feedDao
  val sourceDao = dbLayer.sourceDao

  section("feedDao")
  "feed dao" should {
    "find unread" in new FeedsSpec(3) {
      feedDao.findUnread(id) must contain(atLeast(feeds : _*)).await
      val feedId = Await.result(feedDao.findBySource(id).map(_.head), duration).id.get

      feedDao.modifyRead(feedId, true) must be_==(1).await

      feedDao.findUnread(id) must contain(atLeast(feeds.slice(1, 3) : _*)).await
    }

    "find count" in new FeedsSpec(10) {
      feedDao.feedCountBySourceId(id) must be_==(10).await
    }

    "delete feeds" in new FeedsSpec(10) {
      feedDao.feedCountBySourceId(id) must be_==(10).await

      feedDao.deleteFeedsBySource(id) must be_==(10).await

      feedDao.feedCountBySourceId(id) must be_==(0).await
    }

    "mark all" in new FeedsSpec(9) {
      val anotherSourceId = Gen.genLong
      insertSource(Gen.genSource(Some(anotherSourceId)))
      insertFeed(Gen.genFeed(anotherSourceId, "some-url"))

      // 9 + 1 = 10 + from another specs
      feedDao.markAll must be_>(10).await

      feedDao.findUnread(id).map(_.size) must be_==(0).await

      feedDao.findUnread(anotherSourceId).map(_.size) must be_==(0).await
    }

    "mark by source" in new FeedsSpec(10) {
      val anotherSourceId = Gen.genLong
      insertSource(Gen.genSource(Some(anotherSourceId)))
      insertFeed(Gen.genFeed(anotherSourceId, "some-url"))

      feedDao.markBySource(id) must be_==(10).await

      feedDao.findUnread(id).map(_.size) must be_==(0).await

      feedDao.findUnread(anotherSourceId).map(_.size) must be_==(1).await
    }

    "last n" in new FeedsSpec(10) {
      feedDao.lastN(1).map(_.size) must be_==(1).await
    }

    "page for source" in new FeedsSpec(10) {
      val n = feeds.sortBy(_.publishedDate).reverse.slice(7, 100)
      feedDao.pageForSource(id, 7, 100).map { xs =>
        xs.flatMap(_.id)
      } must contain(allOf(n.flatMap(_.id) : _*)).await
    }

    "favorites" in new FeedsSpec(3) {
      feedDao.modifyFav(ids.head, true) must be_==(1).await
      feedDao.favorites.map(_.map(_.id)) must contain(feeds.head.id).await
    }

    "update content" in new FeedsSpec(1) {
      val feedId = ids.head

      val content = "foo bar baz"

      feedDao.updateContent(feedId, content) must be_==(1).await

      feedDao.findOne(feedId).map(x => x.map(_.content)) must beSome(Option(content)).await
    }

    "feeds by source" in new FeedsSpec(1) {
      feedDao.feedBySourceCount(false).map{ xs => xs.toMap.get(id).get } must be_==(1).await
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

      feedDao.mergeFeeds(id, entry :: newEntry :: Nil)
        .map(xs => xs.size) must be_==(1).await

      feedDao.findOne(ids.head)
        .map(x => x.map(_.description)) must beSome(entry.description).await
    }

  }


  private class FeedsSpec(count: Int = 0) extends Scope {
    private val tmpSource = Gen.genSource()
    val id = Gen.genLong
    insertSource(tmpSource.copy(id = Some(id)))
    val source = tmpSource.copy(id = Some(id))

    val ids = scala.collection.mutable.ArrayBuffer[Long]()

    val feeds = (0 until count).map { index =>
      val feed = Gen.genFeed(id, source.url)
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
