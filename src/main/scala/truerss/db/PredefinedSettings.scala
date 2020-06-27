package truerss.db

import truerss.dto.SetupKey

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

case class PredefinedSettings(key: String, description: String, value: SettingValue) {
  def toKey: SetupKey = {
    SetupKey(key, description)
  }
}

object Predefined {
  val fFeedParallelism = "parallelism"
  val fReadContent = "read_content"
  val fFeedsPerPage = "feeds_per_page"
  val fShortView = "short_view"

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

  val feedsPerPage = PredefinedSettings(
    key = fFeedsPerPage,
    description = "Feeds per page",
    value = SelectableValue(Iterable(1, 10, 20, 30, 50, 100), 10)
  )

  val shortView = PredefinedSettings(
    key = fShortView,
    description = "Display only feeds titles",
    value = RadioValue(false)
  )

  val predefined = parallelism :: read :: feedsPerPage :: shortView :: Nil
}