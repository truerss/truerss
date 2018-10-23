package net.truerss.services

import java.util.Date

import com.github.truerss.base.Entry
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.{BeforeAfterAll, Scope}
import truerss.db.{Feed, Source}

import scala.concurrent.Await
import scala.concurrent.duration._

class FeedDaoTest(implicit ee: ExecutionEnv)
  extends FullDbHelper with SpecificationLike with BeforeAfterAll {

  override def dbName = "fee_dao_spec"

  sequential

  implicit val duration = 3 seconds


  val feedDao = dbLayer.feedDao
  val sourceDao = dbLayer.sourceDao

  section("feedDao")
  "feed dao" should {
    "find unread" in new feedsSpec(3) {
      feedDao.findUnread(id) must contain(atLeast(feeds : _*)).await

      val feedId = ids.head

      feedDao.modifyRead(feedId, true) must be_==(1).await

      feedDao.findUnread(id) must contain(atLeast(feeds.slice(1,3) : _*)).await
    }

    "find count" in new feedsSpec(10) {
      feedDao.feedCountBySourceId(id) must be_==(10).await
    }

    "delete feeds" in new feedsSpec(10) {
      feedDao.feedCountBySourceId(id) must be_==(10).await

      feedDao.deleteFeedsBySource(id) must be_==(10).await

      feedDao.feedCountBySourceId(id) must be_==(0).await
    }

    "mark all" in new feedsSpec(9) {
      val anotherSourceId = insertSource()
      insertFeed(Gen.genFeed(anotherSourceId, "some-url"))

      // 9 + 1 = 10 + from another specs
      feedDao.markAll must be_>(10).await

      feedDao.findUnread(id).map(_.size) must be_==(0).await

      feedDao.findUnread(anotherSourceId).map(_.size) must be_==(0).await
    }

    "mark by source" in new feedsSpec(10) {
      val anotherSourceId = insertSource()
      insertFeed(Gen.genFeed(anotherSourceId, "some-url"))

      feedDao.markBySource(id) must be_==(10).await

      feedDao.findUnread(id).map(_.size) must be_==(0).await

      feedDao.findUnread(anotherSourceId).map(_.size) must be_==(1).await
    }

    "last n" in new feedsSpec(10) {
      feedDao.lastN(1).map(_.size) must be_==(1).await
    }

    "page for source" in new feedsSpec(10) {
      val n = feeds.sortBy(_.publishedDate).reverse.slice(7, 100)
      feedDao.pageForSource(id, 7, 100) must contain(allOf(n : _*)).await
    }

    "favorites" in new feedsSpec(3) {
      feedDao.modifyFav(ids.head, true) must be_==(1).await
      feedDao.favorites.map(_.map(_.id)) must contain(feeds.head.id).await
    }

    "update content" in new feedsSpec(1) {
      val feedId = ids.head

      val content = "foo bar baz"

      feedDao.updateContent(feedId, content) must be_==(1).await

      feedDao.findOne(feedId).map(x => x.map(_.content)) must beSome(Option(content)).await
    }

    "feeds by source" in new feedsSpec(1) {
      feedDao.feedBySourceCount(false).map{ xs => xs.toMap.get(id).get } must be_==(1).await
    }

    "merge feeds - update old feeds and insert new" in new feedsSpec(3) {
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
        .map(xs => xs.size) must be_==(2).await

      w

      feedDao.findOne(ids.head)
        .map(x => x.map(_.description)) must beSome(entry.description).await

    }

  }


  class feedsSpec(count: Int = 0) extends Scope {
    private val tmpSource = Gen.genSource()
    val id = insertSource(tmpSource)
    val source = tmpSource.copy(id = Some(id))

    val ids = scala.collection.mutable.ArrayBuffer[Long]()

    val feeds = (0 until count).map { index =>
      val feed = Gen.genFeed(id, source.url)
      val tmpId = insertFeed(feed)
      ids += tmpId
      feed.copy(id = Some(tmpId))
    }
  }


  def insertSource(source: Source = Gen.genSource()) = {
    Await.result(sourceDao.insert(source), duration)
  }

  def insertFeed(feed: Feed) = {
    Await.result(feedDao.insert(feed), duration)
  }


}
