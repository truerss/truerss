package truerss.models

case class Notify(level: NotifyLevels.Level, message: String)

object Notify {
  def info(message: String): Notify = {
    Notify(NotifyLevels.Info, message)
  }
  def warning(message: String): Notify = {
    Notify(NotifyLevels.Warning, message)
  }
  def danger(message: String): Notify = {
    Notify(NotifyLevels.Danger, message)
  }
}

object NotifyLevels {
  sealed trait Level { val name: String }
  case object Info extends Level { val name = "info" }
  case object Warning extends Level { val name = "warning" }
  case object Danger extends Level { val name = "danger" }
}