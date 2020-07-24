package net.truerss.dao

import java.util.Date

import com.github.truerss.base.Entry
import net.truerss.Gen
import org.specs2.mutable.Specification
import truerss.db.FeedDao

class FeedDaoTests extends Specification {

  "FeedDao" should {
    "#calc" in {
      val sourceId = 1
      val feed1 = Gen
        .genFeed(sourceId, "http://example.com/rss1")
        .copy(id = Some(1L))
      val feed2 = Gen
        .genFeed(sourceId, "http://example.com/rss2")
        .copy(id = Some(10L))
      val feed3 = Gen
        .genFeed(sourceId, "http://example.com/rss3")
        .copy(id = Some(100L))

      val entry1 = Entry(
        url = feed1.url,
        title = feed1.title,
        author = "author",
        publishedDate = new Date(),
        description = None,
        content = None,
        forceUpdate = false
      )

      val entry2 = Entry(
        url = feed2.url,
        title = feed2.title,
        author = "author",
        publishedDate = new Date(),
        description = None,
        content = None,
        forceUpdate = true
      )

      val entry3 = Entry(
        url = Gen.genUrl,
        title = "abc",
        author = "author",
        publishedDate = new Date(),
        description = None,
        content = None,
        forceUpdate = false
      )

      val result = FeedDao.calculate(sourceId,
        Iterable(entry1, entry2, entry3),
        Iterable(feed1, feed2, feed3)
      )

      result.feedsToInsert.map(_.url) must contain(allOf(entry3.url))

      result.feedsToUpdateByUrl.map(_.url) must contain(allOf(entry2.url))
    }
  }

}
