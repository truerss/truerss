package truerss.db

import slick.dbio.{DBIOAction, NoStream}
import slick.jdbc.JdbcBackend
import zio.Task

object JdbcTaskSupport {

  implicit class JdbcBackendExt(val db: JdbcBackend.DatabaseDef) extends AnyVal {
    def go[T](action: DBIOAction[T, NoStream, Nothing]): Task[T] = {
      Task.fromFuture { implicit ec => db.run(action) }
    }
  }

}
