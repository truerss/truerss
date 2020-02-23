package truerss.db

import slick.jdbc.JdbcBackend.DatabaseDef
import truerss.db.driver.CurrentDriver

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class UserSettingsDao(val db: DatabaseDef)(implicit
                                           val ec: ExecutionContext,
                                           driver: CurrentDriver) {

  import driver.profile.api._
  import driver.query.userSettings

  def getSettings: Future[Iterable[UserSettings]] = {
    db.run(userSettings.result)
  }

  def getByKey(key: String): Future[Option[UserSettings]] = {
    db.run(userSettings.filter(_.key === key).take(1).result)
      .map(_.headOption)
  }

  def update[T: ClassTag](key: String, value: T): Future[Int] = {
    val q = userSettings.filter(_.key === key)
    val statement = value match {
      case x: Int => q.map(_.valueInt).update(Some(x))
      case x: Boolean => q.map(_.valueBoolean).update(Some(x))
      case x: String => q.map(_.valueString).update(Some(x))
      case _ => throw new IllegalStateException(s"Unexpected type, for $key, value: $value")
    }
    db.run(statement)
  }

  def insert(xs: Iterable[UserSettings]): Future[Option[Int]] = {
    db.run {
      userSettings ++= xs
    }
  }

}
