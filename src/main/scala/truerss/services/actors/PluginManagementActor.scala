package truerss.services.actors

import akka.actor.Props

import truerss.api.{CssResponse, JsResponse, ModelResponse}
import truerss.util.ApplicationPlugins

/**
  * Created by mike on 4.5.17.
  */
class PluginManagementActor(appPlugins: ApplicationPlugins) extends CommonActor {

  import PluginManagementActor._

  override def defaultHandler: Receive = {
    case GetJs =>
      sender ! JsResponse(appPlugins.js.mkString)

    case GetCss =>
      sender ! CssResponse(appPlugins.css.mkString)

    case GetPluginList =>
      sender ! ModelResponse(appPlugins)
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
