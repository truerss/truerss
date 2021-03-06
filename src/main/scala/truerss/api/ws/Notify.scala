package truerss.api.ws

case class Notify(message: String,
                  level: NotifyLevel.Value
                 )

object NotifyLevel extends Enumeration {
  val Info, Warning, Danger = Value
}
