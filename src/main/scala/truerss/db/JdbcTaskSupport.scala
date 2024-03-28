package truerss.db

import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.jdbc.JdbcBackend
import truerss.services.NotFoundError
import zio.{IO, Task, ZIO}

object JdbcTaskSupport {

  implicit class JdbcBackendExt(val db: JdbcBackend.DatabaseDef) extends AnyVal {
    def go[T](action: DBIOAction[T, NoStream, Nothing]): Task[T] = {
      ZIO.fromFuture { implicit ec => db.run(action) }
    }
  }

  implicit class DBIOActionExt[+R, +S <: NoStream, -E <: Effect](val action: DBIOAction[R, S, E]) extends AnyVal {
    def ~>(db: JdbcBackend.DatabaseDef) = {
      db.go(action)
    }
  }

  implicit class TaskOptExt[T](val x: Task[Option[T]]) extends AnyVal {
    def <~(entityId: Long): IO[NotFoundError, T] = {
      val tmp = for {
        entityOpt <- x
        opt <- ZIO.fromOption(entityOpt)
      } yield opt
      tmp.mapError {
        case None =>
          NotFoundError(entityId)
      }
    }
  }

}
