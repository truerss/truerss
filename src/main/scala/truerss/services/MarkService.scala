package truerss.services

import truerss.db.DbLayer
import zio.Task

class MarkService(private val dbLayer: DbLayer) {

  def markAll: Task[Unit] = {
    dbLayer.feedDao.markAll.unit
  }

  def markOne(sourceId: Long): Task[Unit] = {
    for {
      _ <- dbLayer.sourceDao.findOne(sourceId)
      _ <- dbLayer.feedDao.markBySource(sourceId)
    } yield ()
  }

}
