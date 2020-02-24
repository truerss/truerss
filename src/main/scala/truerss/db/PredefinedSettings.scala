package truerss.db

// I save setting in json representation
// real Setup[T] Keys/Values will be defined on service-level
sealed trait SettingValue {
  type T
  val defaultValue: T
  val name: String
}

case class SelectableValue(predefined: Iterable[Int], defaultValue: Int) extends SettingValue {
  override type T = Int
  override val name: String = SelectableValue.fName
}

object SelectableValue {
  val fName = "selectable"
  val empty = SelectableValue(Nil, 0)
}

case class RadioValue(defaultValue: Boolean) extends SettingValue {
  override type T = Boolean
  override val name: String = RadioValue.fName

  def isYes: Boolean = defaultValue
  def isNo: Boolean = !isYes
}

object RadioValue {
  val fName = "radio"
}

case class PredefinedSettings(key: String, description: String, value: SettingValue)

object Predefined {
  val fFeedParallelism = "parallelism"
  val fReadContent = "read_content"

  val parallelism = PredefinedSettings(
    key = fFeedParallelism,
    description = "Number of simultaneous requests",
    value = SelectableValue(Iterable(10, 25, 45, 100), 10)
  )

  val read = PredefinedSettings(
    key = fReadContent,
    description = "Skip content",
    value = RadioValue(true)
  )

  val predefined = parallelism :: read :: Nil
}