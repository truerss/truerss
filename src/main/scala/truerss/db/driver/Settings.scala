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
  val name: String
  def isUnknown: Boolean = false
}
case object ReadContent extends SettingKey {
  override val name: String = "read_content"
}

case class UnknownKey(key: String) extends SettingKey {
  override val name: String = key

  override def isUnknown: Boolean = true
}

case class Settings(key: SettingKey, value: SettingValue)

sealed trait UserSelectedValue[T] {
  def value: T
  def tpe = classOf[T]
}
case class UserSelectedIntValue(value: Int) extends UserSelectedValue[Int]
case class UserSelectedBoolValue(value: Boolean) extends UserSelectedValue[Boolean]

case class UserSettings[T](key: Settings, value: UserSelectedValue[T])

object Settings {
  final val readContent = Settings(ReadContent, CheckBoxValue(true))
  val predefined = Iterable(
    readContent
  )
}


