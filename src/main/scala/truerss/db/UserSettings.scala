package truerss.db

case class UserSettings(key: String,
                        valueInt: Option[Int],
                        valueBoolean: Option[Boolean],
                        valueString: Option[String]
                       )
