package truerss.db


import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.CurrentDriver
import truerss.services.NotFoundError
import zio.{IO, Task}

class SourceStatusesDao(val db: DatabaseDef)(implicit
                                             driver: CurrentDriver
) {
  import JdbcTaskSupport._
  import driver.profile.api._
  import driver.query.sourceStatuses

  def all: Task[Seq[SourceStatus]] = {
    sourceStatuses.result ~> db
  }

  def findOne(sourceId: Long): IO[NotFoundError, SourceStatus] = {
    sourceStatuses.filter(_.sourceId === sourceId)
      .take(1).result.headOption ~> db <~ sourceId
  }

  def insertOne(sourceId: Long): Task[Unit] = {
    ((sourceStatuses += SourceStatus(sourceId, 0)) ~> db).unit
  }

  def delete(sourceId: Long): Task[Unit] = {
    (sourceStatuses.filter(_.sourceId === sourceId).delete ~> db).unit
  }

  def resetErrors(sourceId: Long): Task[Unit] = {
    (sourceStatuses.filter(_.sourceId === sourceId)
      .map(_.errorCount).update(0) ~> db).unit
  }

  def incrementError(sourceId: Long): Task[Unit] = {
    Task.fromFuture { implicit ec =>
      val action = (for {
        currentT <- sourceStatuses
          .filter(_.sourceId === sourceId)
          .map(_.errorCount)
          .take(1)
          .result
          .headOption

        _ <- currentT.headOption.map { current =>
          sourceStatuses
            .filter(_.sourceId === sourceId)
            .map(_.errorCount)
            .update(current + 1)
        }.getOrElse {
          sourceStatuses += SourceStatus(sourceId, 1)
        }
      } yield ()).transactionally

      db.run(action)
    }
  }

}
