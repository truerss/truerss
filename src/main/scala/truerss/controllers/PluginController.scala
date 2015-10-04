package truerss.controllers

import com.github.fntzr.spray.routing.ext.BaseController

import truerss.system.plugins

trait PluginController  extends BaseController with ProxyRefProvider
with ResponseHelper with ActorRefExt with FilesProvider {

  import plugins.GetPluginList

  import spray.routing.HttpService._

  def all = end(GetPluginList)

  def js = {
    complete(jsFiles.mkString)
  }

  def css = {
    complete(cssFiles.mkString)
  }
}
