package truerss.db.driver

sealed trait SupportedDb
case object Postgresql extends SupportedDb
case object Sqlite extends SupportedDb
case object Mysql extends SupportedDb