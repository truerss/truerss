package net.truerss.services

import java.time.{LocalDateTime, ZoneOffset}
import java.util.Date

import net.truerss.Gen
import org.specs2.mutable.Specification
import truerss.db.Feed
import truerss.services.SourceOverviewService

class SourceOverviewServiceTest extends Specification {

  import SourceOverviewService._
  import SourceOverviewServiceTest._

  private val sourceId = 1L
  private val sourceUrl = "http://example.com/rss"
  private val feed = Gen.genFeed(sourceId, sourceUrl)
  private val date = LocalDateTime.now(ZoneOffset.UTC)


  "calculate overview" should {
    "stats" in {
      val feeds = Vector(
        feed.withId(1).asRead.asFav,
        feed.withId(2).asUnRead.asFav,
        feed.withId(3).asRead.asUnFav,
        feed.withId(4).asUnRead.asUnFav
      )
      val result = SourceOverviewService.calculate(1L, feeds)
      result.favoritesCount ==== 2
      result.sourceId ==== sourceId
      result.feedsCount ==== feeds.size
      result.unreadCount ==== 2
    }

    "frequency in days" in {
      val feeds = (0 to 6).map { x =>
        feed.withId(x+1).withDate(date.plusDays(x))
      }

      val result = SourceOverviewService.calculateFrequency(feeds)
      result.perDay must be_>(1d)
    }

    "frequency in weeks" in {
      val feeds = (0 to 6).map { x =>
        feed.withId(x+1).withDate(date.plusDays(x))
      }

      val result = SourceOverviewService.calculateFrequency(feeds)
      result.perWeek must be_>(7d)
    }

    "frequency in months" in {
      val feeds = (0 to 4).map { x =>
        feed.withId(x+1).withDate(date.plusMonths(x))
      }
      val result = SourceOverviewService.calculateFrequency(feeds)

      result.perMonth must be_>(1d)
    }
  }



}

object SourceOverviewServiceTest {
  implicit class FeedTestExt(val f: Feed) extends AnyVal {
    def withId(id: Long): Feed = {
      f.copy(id = Some(id))
    }

    def withDate(date: LocalDateTime): Feed = {
      f.copy(publishedDate = date)
    }

    def asUnRead: Feed = {
      f.copy(read = false)
    }

    def asRead: Feed = {
      f.copy(read = true)
    }

    def asFav: Feed = {
      f.copy(favorite = true)
    }

    def asUnFav: Feed = {
      f.copy(favorite = false)
    }
  }
}
