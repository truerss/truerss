package truerss.services.management

import truerss.api.{AppPluginsResponse, CssResponse, JsResponse}
import truerss.services.ApplicationPluginsService
import zio.Task

class PluginsManagement(val service: ApplicationPluginsService) extends BaseManagement {

  def getJs: Z = {
    Task.effectTotal(JsResponse(service.js.mkString))
  }

  def getCss: Z = {
    Task.effectTotal(CssResponse(service.css.mkString))
  }

  def getPluginList: Z = {
    Task.effectTotal(AppPluginsResponse(service.view))
  }

}