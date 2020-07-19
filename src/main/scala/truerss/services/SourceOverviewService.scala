package truerss.services

import java.time.{LocalDateTime, ZoneOffset}
import java.util.concurrent.TimeUnit

import truerss.db.{DbLayer, Feed}
import truerss.dto.{FeedsFrequency, SourceOverview}
import truerss.services
import truerss.services.SourceOverviewServiceZ.SourceOverviewServiceZ
import zio._

import scala.concurrent.ExecutionContext

object SourceOverviewServiceZ {

  type SourceOverviewServiceZ = Has[Service]

  trait Service {
    def getSourceOverview(sourceId: Long): Task[SourceOverview]
  }

  final class Live(private val dbLayer: DbLayer) extends Service {
    import SourceOverviewService.calculate
    def getSourceOverview(sourceId: Long): Task[SourceOverview] = {
      // todo compare in-memory vs diff in-db functions
      dbLayer.feedDao.findBySource(sourceId).map(calculate(sourceId, _))
    }
  }

  def getSourceOverview(sourceId: Long): ZIO[SourceOverviewServiceZ, Throwable, SourceOverview] = {
    ZIO.accessM(_.get.getSourceOverview(sourceId))
  }

}

class SourceOverviewService(val dbLayer: DbLayer)(implicit ec: ExecutionContext) {

  private val layer: Layer[Nothing, SourceOverviewServiceZ] =
    ZLayer.succeed(new services.SourceOverviewServiceZ.Live(dbLayer))

  def getSourceOverview(sourceId: Long): Task[SourceOverview] = {
    val f: ZIO[SourceOverviewServiceZ, Throwable, SourceOverview] =
      SourceOverviewServiceZ.getSourceOverview(sourceId)

    f.provideLayer(layer)
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

  def calculateFrequency(feeds: Seq[Feed]): FeedsFrequency = {
    if (feeds.isEmpty) {
      FeedsFrequency.empty
    } else {
      val count = feeds.length
      val dates = feeds.map(_.publishedDate).sorted
      val start = dates.head.toInstant(ZoneOffset.UTC).getEpochSecond
      val end = dates.last.toInstant(ZoneOffset.UTC).getEpochSecond

      val diff = scala.math.abs(end - start)
      val daysDiff = TimeUnit.SECONDS.toDays(diff) * 1.0

      val perDay = count / daysDiff
      val perWeek = count / (daysDiff / 7.0)
      val perMonth = count / (daysDiff / 30.0)

      FeedsFrequency(
        perDay = perDay,
        perWeek = perWeek,
        perMonth = perMonth
      )
    }
  }

}