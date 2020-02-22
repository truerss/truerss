package truerss.services.management

import truerss.api.{AppPluginsResponse, CssResponse, JsResponse}
import truerss.services.ApplicationPluginsService

class PluginsManagement(val service: ApplicationPluginsService) {

  def getJs: JsResponse = {
    JsResponse(service.js.mkString)
  }

  def getCss: CssResponse = {
    CssResponse(service.css.mkString)
  }

  def getPluginList: AppPluginsResponse = {
    AppPluginsResponse(service.view)
  }

}