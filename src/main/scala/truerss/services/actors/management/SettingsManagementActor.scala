package truerss.services.actors.management

import akka.actor.Props
import akka.pattern.pipe
import truerss.services.GlobalSettingsService

class SettingsManagementActor(globalSettingsService: GlobalSettingsService) extends CommonActor {

  import SettingsManagementActor._
  import context.dispatcher

  def receive = {
    case GetSettings =>
      globalSettingsService.getSettings pipeTo sender
  }

}

object SettingsManagementActor {
  def props(globalSettingsService: GlobalSettingsService) = {
    Props(classOf[SettingsManagementActor], globalSettingsService)
  }

  sealed trait SettingsMessage
  case object GetSettings extends SettingsMessage
}
