package truerss.db

// I save setting in json representation
// real Setup[T] Keys/Values will be defined on service-level
sealed trait SettingValue {
  val name: String
}

case class SelectableValue(predefined: Iterable[Int]) extends SettingValue {
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

case class PredefinedSettings(key: String, description: String, value: SettingValue)
