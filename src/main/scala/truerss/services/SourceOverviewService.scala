package truerss.services

import truerss.db.{DbLayer, Feed}
import truerss.dto.{FeedsFrequencyDto, SourceOverview}
import truerss.util.FrequencyUtil
import zio.Task

import java.time.LocalDateTime

class SourceOverviewService(private val dbLayer: DbLayer) {

  import SourceOverviewService.calculate

  // todo check performance vs sql queries
  def getSourceOverview(sourceId: Long): Task[SourceOverview] = {
    dbLayer.sourceDao.findOne(sourceId) *>
    dbLayer.feedDao.findBySource(sourceId).map(calculate(sourceId, _))
  }

}

object SourceOverviewService {

  implicit val localDateTimeOrdering: Ordering[LocalDateTime] =
    (x: LocalDateTime, y: LocalDateTime) => {
      if (x.isBefore(y)) {
        -1
      } else if (x.isAfter(y)) {
        1
      } else {
        0
      }
  }

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

  def calculateFrequency(feeds: Seq[Feed]): FeedsFrequencyDto = {
    if (feeds.isEmpty) {
      FeedsFrequencyDto.empty
    } else {
      val count = feeds.length
      val dates = feeds.map(_.publishedDate).sorted
      val perDay = FrequencyUtil.calculatePerDay(count, dates.head, dates.last)

      FeedsFrequencyDto(
        perDay = perDay.value,
        perWeek = perDay.perWeek,
        perMonth = perDay.perMonth
      )
    }
  }

}