package truerss.services.actors.management

import akka.actor.Props
import truerss.api.{AppPluginsResponse, CssResponse, JsResponse}
import truerss.services.ApplicationPluginsService
import truerss.util.ApplicationPlugins

/**
  * Created by mike on 4.5.17.
  */
class PluginManagementActor(appPluginService: ApplicationPluginsService) extends CommonActor {

  import PluginManagementActor._

  override def receive: Receive = {
    case GetJs =>
      sender ! JsResponse(appPluginService.js.mkString)

    case GetCss =>
      sender ! CssResponse(appPluginService.css.mkString)

    case GetPluginList =>
      sender ! AppPluginsResponse(appPluginService.view)
  }
}

object PluginManagementActor {
  def props(appPlugins: ApplicationPlugins) = {
    Props(classOf[PluginManagementActor], appPlugins)
  }

  sealed trait PluginManagementMessage
  case object GetPluginList extends PluginManagementMessage
  case object GetJs extends PluginManagementMessage
  case object GetCss extends PluginManagementMessage
}
