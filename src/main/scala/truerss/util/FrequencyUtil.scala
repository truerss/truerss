package truerss.util

import java.time.{LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit

object FrequencyUtil {

  case class PerDay(value: Double, daysDiff: Double, isEmpty: Boolean) {
    def perWeek: Double = {
      if (isEmpty) {
        0d
      } else {
        value / (daysDiff / 7d)
      }
    }

    def perMonth: Double = {
      if (isEmpty) {
        0d
      } else {
        value / (daysDiff / 30d)
      }
    }
  }
  object PerDay {
    val empty: PerDay = PerDay(0d, 0d, isEmpty = true)
  }

  def calculatePerDay(count: Int, startDate: LocalDateTime, endDate: LocalDateTime): PerDay = {
    val start = startDate.toInstant(ZoneOffset.UTC).getEpochSecond
    val end = endDate.toInstant(ZoneOffset.UTC).getEpochSecond
    val diff = scala.math.abs(end - start)
    val daysDiff = TimeUnit.SECONDS.toDays(diff) * 1.0
    if (daysDiff < 1) {
      PerDay.empty
    } else {
      PerDay(count / daysDiff, daysDiff, isEmpty = false)
    }
  }


}
