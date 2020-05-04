package truerss.services

import java.util.concurrent.TimeUnit

import truerss.db.{DbLayer, Feed}
import truerss.dto.{FeedsFrequency, SourceOverview}

import scala.concurrent.{ExecutionContext, Future}

class SourceOverviewService(val dbLayer: DbLayer)(implicit ec: ExecutionContext) {

  import SourceOverviewService._

  private val dao = dbLayer.feedDao

  def getSourceOverview(sourceId: Long): Future[SourceOverview] = {
    // todo compare in-memory vs diff in-db functions
    dao.findBySource(sourceId).map { feeds =>
      calculate(sourceId, feeds)
    }
  }

}

object SourceOverviewService {
  def calculate(sourceId: Long, feeds: Seq[Feed]): SourceOverview = {
    val favorites = feeds.count(_.favorite)
    val unread = feeds.count(!_.read)
    val count = feeds.length

    SourceOverview(
      sourceId = sourceId,
      unreadCount = unread,
      favoritesCount = favorites,
      feedsCount = count,
      frequency = calculateFrequency(feeds)
    )
  }

  def calculateFrequency(feeds: Seq[Feed]): FeedsFrequency = {
    if (feeds.isEmpty) {
      FeedsFrequency.empty
    } else {
      val count = feeds.length
      val dates = feeds.map(_.publishedDate).sorted
      val start = dates.head.getTime
      val end = dates.last.getTime

      val diff = scala.math.abs(end - start)
      val daysDiff = TimeUnit.MILLISECONDS.toDays(diff) * 1.0

      val perDay = daysDiff/count
      val perWeek = daysDiff/(count*7)
      val perMonth = daysDiff/(count*30)

      FeedsFrequency(
        perDay = perDay,
        perWeek = perWeek,
        perMonth = perMonth
      )
    }
  }

}