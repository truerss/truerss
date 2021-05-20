package truerss.services

import truerss.db.{DbLayer, SourceUpdatingFrequency, StartEndDates}
import truerss.util.FrequencyUtil
import zio.Task

class SourceUpdatingFrequencyService(val dbLayer: DbLayer) {

  import dbLayer._
  import SourceUpdatingFrequencyService._

  // called after every update
  def recalculateFrequency(sourceId: Long): Task[Unit] = {
    for {
      count <- feedDao.findFeedCount(sourceId)
      startEndT <- feedDao.findStartEndDates(sourceId)
      perDay = calculateFrequency(count, startEndT)
      _ <- frequencyDao.updateFrequency(SourceUpdatingFrequency(sourceId, perDay.value))
    } yield ()
  }

}

object SourceUpdatingFrequencyService {
  import FrequencyUtil._

  def calculateFrequency(count: Int, startEndT: Option[StartEndDates]): PerDay = {
    startEndT.map { dates =>
      FrequencyUtil.calculatePerDay(
        count = count,
        startDate = dates.start,
        endDate = dates.end
      )
    }.getOrElse(PerDay.empty)
  }
}