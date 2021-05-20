package truerss.services

import truerss.db.{DbLayer, SourceStatus}
import truerss.dto.SourceStatusDto
import zio.Task

class SourceStatusesService(private val dbLayer: DbLayer) {

  import SourceStatusesService._

  def findAll: Task[Iterable[SourceStatusDto]] = {
    dbLayer.sourceStatusesDao.all.map { xs => xs.map(_.toDto) }
  }

  def findOne(sourceId: Long): Task[SourceStatusDto] = {
    dbLayer.sourceStatusesDao.findOne(sourceId).map(_.toDto)
  }

  def incrementError(sourceId: Long): Task[Unit] = {
    dbLayer.sourceStatusesDao.incrementError(sourceId)
  }

}

object SourceStatusesService {
  implicit class SourceStatusExt(val x: SourceStatus) extends AnyVal {
    def toDto: SourceStatusDto = {
      SourceStatusDto(
        sourceId = x.sourceId,
        errorCount = x.errorCount
      )
    }
  }
}
