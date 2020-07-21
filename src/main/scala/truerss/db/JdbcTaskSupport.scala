package truerss.db

import slick.dbio.{DBIOAction, Effect, NoStream}
import slick.jdbc.JdbcBackend
import zio.Task

object JdbcTaskSupport {

  implicit class JdbcBackendExt(val db: JdbcBackend.DatabaseDef) extends AnyVal {
    def go[T](action: DBIOAction[T, NoStream, Nothing]): Task[T] = {
      Task.fromFuture { implicit ec => db.run(action) }
    }
  }

  implicit class DBIOActionExt[+R, +S <: NoStream, -E <: Effect](val action: DBIOAction[R, S, E]) extends AnyVal {
    def ~>(db: JdbcBackend.DatabaseDef) = {
      db.go(action)
    }
  }

}
