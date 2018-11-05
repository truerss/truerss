package truerss.db.driver

sealed trait SettingValue {
  val name: String
}

// user input ?

case class SelectableValue(predefined: Iterable[String]) extends SettingValue {
  override val name: String = SelectableValue.fName
}

object SelectableValue {
  val fName = "selectable"
  val empty = SelectableValue(Nil)
}

case class GlobalSettings(key: String, value: SettingValue)


