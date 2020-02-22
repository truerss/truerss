package truerss.db.driver

sealed trait SettingValue {
  val name: String
}

case class SelectableValue(predefined: Iterable[String]) extends SettingValue {
  override val name: String = SelectableValue.fName
}

object SelectableValue {
  val fName = "selectable"
  val empty = SelectableValue(Nil)
}

case class CheckBoxValue(currentState: Boolean) extends SettingValue {
  override val name: String = CheckBoxValue.fName

  def isYes: Boolean = currentState
  def isNo: Boolean = !isYes
}

object CheckBoxValue {
  val fName = "checkbox"
  val empty = CheckBoxValue(false)
}

sealed trait SettingKey {
  type T
  val name: String
  def isUnknown: Boolean = false
}
case object ReadContent extends SettingKey {
  override val name: String = "read_content"
  override type T = Boolean
}

case class UnknownKey(key: String) extends SettingKey {
  override val name: String = key

  override def isUnknown: Boolean = true

  override type T = String
}

case class Settings(key: SettingKey, value: SettingValue)

case class UserSelectedValue[T](value: T)

case class UserSettings(key: SettingKey, value: UserSelectedValue[SettingKey#T])

object PredefinedSettings {
  final val readContent = Settings(ReadContent, CheckBoxValue(true))
  val predefined = Iterable(
    readContent
  )

}


